package dev.vessel.kernel.autoupdate;

/**
 * Specialized runtime exception thrown when transactional migration snapshot storage or metadata
 * persistence encounters an unrecoverable I/O or state error.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public final class SentinelStorageException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@link SentinelStorageException} with detail message.
     *
     * @param message failure explanation
     */
    public SentinelStorageException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@link SentinelStorageException} with detail message and underlying cause.
     *
     * @param message failure explanation
     * @param cause   underlying exception
     */
    public SentinelStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
