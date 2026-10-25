package dev.vessel.kernel.sanitizer;

/**
 * Domain exception thrown when bytecode scanning or class parsing fails during copyright auditing.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class BytecodeScanException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message human-readable detail message
     */
    public BytecodeScanException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and root cause.
     *
     * @param message human-readable detail message
     * @param cause   the root cause of the error
     */
    public BytecodeScanException(String message, Throwable cause) {
        super(message, cause);
    }
}
