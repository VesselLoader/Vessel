package dev.vessel.kernel.autoupdate;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Immutable record encapsulating the result of a dual-slot binary swap operation.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param previousSlot     the active binary slot prior to the swap operation
 * @param activeSlot       the newly activated binary slot after the swap operation
 * @param targetJarPath    the absolute or normalized path to the active binary jar
 * @param sha256Checksum   the SHA-256 digest of the new binary payload
 * @param success          {@code true} if the swap succeeded and pointer file was committed
 * @param statusMessage    human-readable summary or error diagnosis message
 *
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public record SlotSwapResult(
        ServerBinarySlot previousSlot,
        ServerBinarySlot activeSlot,
        Path targetJarPath,
        String sha256Checksum,
        boolean success,
        String statusMessage
) {

    public SlotSwapResult {
        Objects.requireNonNull(previousSlot, "previousSlot must not be null");
        Objects.requireNonNull(activeSlot, "activeSlot must not be null");
        Objects.requireNonNull(targetJarPath, "targetJarPath must not be null");
        Objects.requireNonNull(sha256Checksum, "sha256Checksum must not be null");
        Objects.requireNonNull(statusMessage, "statusMessage must not be null");
    }

    /**
     * Factory method creating a successful {@link SlotSwapResult}.
     *
     * @param previousSlot   previous active slot
     * @param activeSlot     new active slot
     * @param targetJarPath  path to target binary jar
     * @param sha256Checksum verified SHA-256 digest
     * @param message        success description
     * @return populated {@link SlotSwapResult}
     */
    public static SlotSwapResult successful(
            ServerBinarySlot previousSlot,
            ServerBinarySlot activeSlot,
            Path targetJarPath,
            String sha256Checksum,
            String message) {
        return new SlotSwapResult(previousSlot, activeSlot, targetJarPath, sha256Checksum, true, message);
    }

    /**
     * Factory method creating a failed {@link SlotSwapResult}.
     *
     * @param previousSlot   previous active slot
     * @param attemptedSlot  slot attempted to be activated
     * @param targetJarPath  path to attempted target binary jar
     * @param errorMessage   failure explanation
     * @return populated failed {@link SlotSwapResult}
     */
    public static SlotSwapResult failed(
            ServerBinarySlot previousSlot,
            ServerBinarySlot attemptedSlot,
            Path targetJarPath,
            String errorMessage) {
        return new SlotSwapResult(previousSlot, attemptedSlot, targetJarPath, "", false, errorMessage);
    }
}
