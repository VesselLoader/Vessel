package dev.vessel.kernel.autoupdate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Windows-safe server binary swapper leveraging dual-slot executable binaries ({@code server-slot-a.jar}
 * and {@code server-slot-b.jar}) and temporary shadow renaming to circumvent Windows OS file locks.
 *
 * <p>On Windows operating systems, running executable jars are held locked by the JVM with exclusive
 * read access, preventing in-place overwriting. This swapper eliminates lock conflicts by deploying
 * updates to the idle standby slot and cleanly flipping the atomic active slot pointer.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public final class WindowsFileLockSafeSwapper {

    public static final String ACTIVE_SLOT_POINTER_FILE = "current-slot.txt";
    public static final String SWAP_TEMP_SUFFIX = ".vessel-swap.tmp";
    public static final String PENDING_DELETE_SUFFIX = ".vessel-delete.pending";

    public static final int DEFAULT_MAX_RETRIES = 5;
    public static final long DEFAULT_BACKOFF_MILLIS = 200L;
    public static final String SHA256_ALGORITHM = "SHA-256";

    /**
     * Determines the currently active server binary slot by inspecting {@code current-slot.txt}
     * under {@code serverRoot}. Defaults to {@link ServerBinarySlot#SLOT_A} if the file does not exist.
     *
     * @param serverRoot root directory of the server installation
     * @return active {@link ServerBinarySlot}
     */
    public ServerBinarySlot determineActiveSlot(Path serverRoot) {
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");
        Path pointerFile = serverRoot.resolve(ACTIVE_SLOT_POINTER_FILE);
        if (!Files.isRegularFile(pointerFile)) {
            return ServerBinarySlot.SLOT_A;
        }
        try {
            String content = Files.readString(pointerFile, StandardCharsets.UTF_8).trim();
            if (content.isEmpty()) {
                return ServerBinarySlot.SLOT_A;
            }
            return ServerBinarySlot.fromString(content);
        } catch (Exception ignored) {
            return ServerBinarySlot.SLOT_A;
        }
    }

    /**
     * Resolves the standby slot that is currently idle and safe for writing without Windows lock conflicts.
     *
     * @param serverRoot root directory of the server installation
     * @return standby {@link ServerBinarySlot}
     */
    public ServerBinarySlot determineStandbySlot(Path serverRoot) {
        return determineActiveSlot(serverRoot).alternateSlot();
    }

    /**
     * Resolves the full path to the executable jar for a given binary slot.
     *
     * @param serverRoot root directory of the server installation
     * @param slot       target slot
     * @return path to the slot's binary jar file
     */
    public Path resolveSlotBinaryPath(Path serverRoot, ServerBinarySlot slot) {
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");
        Objects.requireNonNull(slot, "slot must not be null");
        return serverRoot.resolve(slot.binaryFileName());
    }

    /**
     * Performs a Windows-safe dual-slot binary swap by writing the new binary payload to the standby slot,
     * verifying its integrity against {@code expectedSha256}, and atomically flipping the active slot pointer.
     *
     * @param serverRoot     root directory of the server installation
     * @param newBinaryBytes byte array containing the new binary executable jar payload
     * @param expectedSha256 expected SHA-256 hex string (or null/empty to skip checksum verification)
     * @return {@link SlotSwapResult} detailing the outcome of the swap
     * @throws IOException if binary staging or pointer atomic write fails
     */
    public SlotSwapResult stageAndSwapToStandbySlot(
            Path serverRoot,
            byte[] newBinaryBytes,
            String expectedSha256) throws IOException {
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");
        Objects.requireNonNull(newBinaryBytes, "newBinaryBytes must not be null");

        ServerBinarySlot currentActive = determineActiveSlot(serverRoot);
        ServerBinarySlot targetStandby = currentActive.alternateSlot();
        Path targetStandbyJar = resolveSlotBinaryPath(serverRoot, targetStandby);

        // Compute and verify SHA-256
        String actualSha256 = computeSha256Hex(newBinaryBytes);
        if (expectedSha256 != null && !expectedSha256.isBlank()) {
            if (!actualSha256.equalsIgnoreCase(expectedSha256.trim())) {
                return SlotSwapResult.failed(
                        currentActive,
                        targetStandby,
                        targetStandbyJar,
                        "SHA-256 digest mismatch. Expected: " + expectedSha256 + ", Actual: " + actualSha256
                );
            }
        }

        // Ensure server root directory exists
        Files.createDirectories(serverRoot);

        // Write to a temporary file first in the same directory to allow atomic replacement
        Path tempStagingFile = serverRoot.resolve(targetStandby.binaryFileName() + SWAP_TEMP_SUFFIX);
        Files.write(
                tempStagingFile,
                newBinaryBytes,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );

        // Move temporary staging file to target standby jar (standby slot is NOT locked by JVM)
        try {
            Files.move(tempStagingFile, targetStandbyJar, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempStagingFile, targetStandbyJar, StandardCopyOption.REPLACE_EXISTING);
        }

        // Atomically flip the current-slot.txt pointer to the newly staged slot
        commitActiveSlotPointer(serverRoot, targetStandby);

        return SlotSwapResult.successful(
                currentActive,
                targetStandby,
                targetStandbyJar,
                actualSha256,
                "Dual-slot swap successfully staged to [" + targetStandby + "]: " + targetStandbyJar.getFileName()
        );
    }

    /**
     * Atomically commits the active slot pointer into {@code current-slot.txt}.
     *
     * @param serverRoot root directory
     * @param newSlot    the newly active slot
     * @throws IOException if writing the pointer file fails
     */
    public void commitActiveSlotPointer(Path serverRoot, ServerBinarySlot newSlot) throws IOException {
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");
        Objects.requireNonNull(newSlot, "newSlot must not be null");

        Path pointerFile = serverRoot.resolve(ACTIVE_SLOT_POINTER_FILE);
        Path tempPointerFile = serverRoot.resolve(ACTIVE_SLOT_POINTER_FILE + SWAP_TEMP_SUFFIX);

        byte[] content = (newSlot.name() + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
        Files.write(
                tempPointerFile,
                content,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );

        try {
            Files.move(tempPointerFile, pointerFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempPointerFile, pointerFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Replaces a locked target file on Windows OS safely using transient shadow renaming and retry backoff.
     *
     * @param targetLockedPath    the path to the file currently locked or potentially locked
     * @param sourceReplacementPath the source file containing replacement content
     * @param maxRetries          maximum retry count
     * @param backoffMillis       exponential/fixed backoff interval in milliseconds
     * @throws WindowsFileLockException if file replacement cannot be completed after max retries
     * @throws IOException              if basic I/O operations fail
     */
    public void replaceLockedFileWithRetry(
            Path targetLockedPath,
            Path sourceReplacementPath,
            int maxRetries,
            long backoffMillis) throws WindowsFileLockException, IOException {
        Objects.requireNonNull(targetLockedPath, "targetLockedPath must not be null");
        Objects.requireNonNull(sourceReplacementPath, "sourceReplacementPath must not be null");

        int retries = Math.max(1, maxRetries);
        long backoff = Math.max(10L, backoffMillis);
        Exception lastException = null;

        for (int attempt = 1; attempt <= retries; attempt++) {
            try {
                // If the target file does not exist yet, simple move is sufficient
                if (!Files.exists(targetLockedPath)) {
                    Files.move(sourceReplacementPath, targetLockedPath, StandardCopyOption.REPLACE_EXISTING);
                    return;
                }

                // Strategy 1: Attempt direct move with REPLACE_EXISTING
                try {
                    Files.move(sourceReplacementPath, targetLockedPath, StandardCopyOption.REPLACE_EXISTING);
                    return;
                } catch (IOException ignored) {
                    // Falls through to shadow rename strategy
                }

                // Strategy 2: Shadow rename locked target to .pending-delete
                Path pendingDeletePath = targetLockedPath.resolveSibling(
                        targetLockedPath.getFileName().toString() + PENDING_DELETE_SUFFIX + "." + System.currentTimeMillis()
                );
                Files.move(targetLockedPath, pendingDeletePath, StandardCopyOption.REPLACE_EXISTING);

                // Move new source into target position
                Files.move(sourceReplacementPath, targetLockedPath, StandardCopyOption.REPLACE_EXISTING);

                // Attempt to clean up pending delete file, or register for exit deletion
                try {
                    Files.deleteIfExists(pendingDeletePath);
                } catch (IOException cleanupEx) {
                    pendingDeletePath.toFile().deleteOnExit();
                }
                return;
            } catch (Exception e) {
                lastException = e;
                if (attempt < retries) {
                    try {
                        Thread.sleep(backoff * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new WindowsFileLockException(targetLockedPath, attempt, "Thread interrupted during backoff", ie);
                    }
                }
            }
        }

        throw new WindowsFileLockException(
                targetLockedPath,
                retries,
                "Unable to swap target locked file across " + retries + " attempts",
                lastException
        );
    }

    /**
     * Computes the SHA-256 hexadecimal string representation of the given byte payload.
     *
     * @param data byte array to hash
     * @return lowercase hexadecimal SHA-256 digest
     */
    public String computeSha256Hex(byte[] data) {
        Objects.requireNonNull(data, "data must not be null");
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA256_ALGORITHM);
            byte[] hashBytes = digest.digest(data);
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Standard SHA-256 message digest algorithm unavailable in JVM", e);
        }
    }
}
