package dev.vessel.kernel.migration;

/**
 * Domain exception thrown when asynchronous chunk disk flush operations encounter I/O failures.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class AsyncFlushException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message human-readable detail message
     */
    public AsyncFlushException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and root cause.
     *
     * @param message human-readable detail message
     * @param cause   the root cause of the error
     */
    public AsyncFlushException(String message, Throwable cause) {
        super(message, cause);
    }
}
