package dev.vessel.kernel.migration;

/**
 * Domain exception thrown when immutable chunk snapshot capture or serialization fails.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ChunkSnapshotException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message human-readable detail message
     */
    public ChunkSnapshotException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and root cause.
     *
     * @param message human-readable detail message
     * @param cause   the root cause of the error
     */
    public ChunkSnapshotException(String message, Throwable cause) {
        super(message, cause);
    }
}
