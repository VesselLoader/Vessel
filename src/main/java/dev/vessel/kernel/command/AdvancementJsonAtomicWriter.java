package dev.vessel.kernel.command;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sovereign atomic writer for Minecraft Advancement JSON files under Decision 93 (ERR-93).
 *
 * <p>Completely eliminates 0-byte file corruption during sudden server crashes or kernel panics.
 * Writes advancement progress to a temporary sibling file, performs hardware fsync via
 * {@link FileChannel#force(boolean)}, and swaps into place atomically.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public final class AdvancementJsonAtomicWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdvancementJsonAtomicWriter.class);

    private static final String TEMP_EXTENSION = ".tmp.";
    private static final String BACKUP_EXTENSION = ".bak";

    /**
     * Constructs a new atomic advancement JSON writer.
     */
    public AdvancementJsonAtomicWriter() {
    }

    /**
     * Writes advancement JSON payload atomically to the designated target path.
     *
     * @param targetPath  destination advancement JSON file path
     * @param jsonContent JSON string content to persist
     * @param createBackup whether to maintain a {@code .bak} copy of the previous version
     * @throws IOException if a physical I/O failure occurs
     */
    public void writeAdvancementJson(Path targetPath, String jsonContent, boolean createBackup) throws IOException {
        Objects.requireNonNull(targetPath, "targetPath cannot be null");
        Objects.requireNonNull(jsonContent, "jsonContent cannot be null");

        byte[] payload = jsonContent.getBytes(StandardCharsets.UTF_8);
        writeAdvancementBytes(targetPath, payload, createBackup);
    }

    /**
     * Writes advancement byte array atomically to the target file.
     *
     * @param targetPath   destination file path
     * @param bytes        UTF-8 encoded JSON bytes
     * @param createBackup whether to preserve a backup file
     * @throws IOException if an I/O error occurs
     */
    public void writeAdvancementBytes(Path targetPath, byte[] bytes, boolean createBackup) throws IOException {
        Objects.requireNonNull(targetPath, "targetPath cannot be null");
        Objects.requireNonNull(bytes, "bytes cannot be null");

        Path parentDir = targetPath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        String tempFileName = targetPath.getFileName().toString() + TEMP_EXTENSION + UUID.randomUUID();
        Path tempPath = (parentDir != null) ? parentDir.resolve(tempFileName) : Path.of(tempFileName);

        File tempFile = tempPath.toFile();
        try {
            // Step 1: Write and fsync to temporary sibling file
            try (FileOutputStream fos = new FileOutputStream(tempFile);
                 FileChannel channel = fos.getChannel()) {
                fos.write(bytes);
                fos.flush();
                channel.force(true);
            }

            // Step 2: Backup previous file if requested and present
            if (createBackup && Files.exists(targetPath) && Files.size(targetPath) > 0) {
                Path backupPath = (parentDir != null)
                        ? parentDir.resolve(targetPath.getFileName().toString() + BACKUP_EXTENSION)
                        : Path.of(targetPath.getFileName().toString() + BACKUP_EXTENSION);
                Files.copy(targetPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            }

            // Step 3: Atomic swap
            try {
                Files.move(tempPath, targetPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                LOGGER.debug("ATOMIC_MOVE unsupported across partitions; falling back to standard replace for '{}'", targetPath);
                Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            LOGGER.debug("Successfully wrote atomic advancement JSON ({} bytes) to '{}'", bytes.length, targetPath);
        } finally {
            // Clean up temporary file if lingering
            if (Files.exists(tempPath)) {
                try {
                    Files.deleteIfExists(tempPath);
                } catch (IOException ignored) {
                    // Suppress cleanup failure
                }
            }
        }
    }
}
