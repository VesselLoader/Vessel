package dev.vessel.kernel.vstm.binary;

import java.util.Objects;

/**
 * Domain-specific runtime exception thrown when a .vstmb binary payload is corrupt,
 * truncated, misaligned, or tampered with.
 *
 * <p>Carries an immutable error code tied to the VESSEL Fatal Errors Catalog
 * (Catalog ref: ERR-0003, UFE-0012).</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 1)
 */
public final class VstmbBinaryCorruptException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;

    /**
     * Constructs a corrupt exception with a standard message and default error code.
     *
     * @param message the detailed description of the corruption
     */
    public VstmbBinaryCorruptException(String message) {
        super(message);
        this.errorCode = "ERR-VSTM-CORRUPT";
    }

    /**
     * Constructs a corrupt exception with a specific error code and description.
     *
     * @param errorCode the catalog error code (e.g., ERR-VSTM-MAGIC, ERR-VSTM-VERSION)
     * @param message   the detailed description of the corruption
     */
    public VstmbBinaryCorruptException(String errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    /**
     * Constructs a corrupt exception with a specific error code, description, and root cause.
     *
     * @param errorCode the catalog error code
     * @param message   the detailed description
     * @param cause     the underlying exception
     */
    public VstmbBinaryCorruptException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    /**
     * Retrieves the immutable catalog error code associated with this failure.
     *
     * @return the catalog error code
     */
    public String getErrorCode() {
        return errorCode;
    }
}
