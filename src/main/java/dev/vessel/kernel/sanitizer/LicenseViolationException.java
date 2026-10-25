package dev.vessel.kernel.sanitizer;

/**
 * Domain exception thrown when clean-room licensing boundaries or copyleft isolation rules are violated.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class LicenseViolationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message human-readable detail message
     */
    public LicenseViolationException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and root cause.
     *
     * @param message human-readable detail message
     * @param cause   the root cause of the error
     */
    public LicenseViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
