package dev.vessel.kernel.identity;

/**
 * Domain exception thrown when metadata parsing fails or contains invalid syntax.
 *
 * @version 1.0.1
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-4)
 */
public class InvalidMetadataException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidMetadataException(String message) {
        super(message);
    }

    public InvalidMetadataException(String message, Throwable cause) {
        super(message, cause);
    }
}
