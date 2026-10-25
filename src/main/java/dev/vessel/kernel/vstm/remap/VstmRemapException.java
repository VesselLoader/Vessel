package dev.vessel.kernel.vstm.remap;

import java.util.Objects;

/**
 * Domain-specific runtime exception thrown when ASM bytecode remapping or JAR transmutation fails.
 *
 * <p>Carries an immutable catalog error code adhering to the VESSEL Fatal Errors Catalog
 * (Catalog ref: ERR-VSTM-REMAP, ERR-0015).</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 3)
 */
public final class VstmRemapException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_ERROR_CODE = "ERR-VSTM-REMAP";

    private final String errorCode;

    /**
     * Constructs a remap exception with a message and the default error code.
     *
     * @param message the detailed description of the remapping error
     */
    public VstmRemapException(String message) {
        super(message);
        this.errorCode = DEFAULT_ERROR_CODE;
    }

    /**
     * Constructs a remap exception with a specific error code and message.
     *
     * @param errorCode the catalog error code
     * @param message   the detailed description
     */
    public VstmRemapException(String errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    /**
     * Constructs a remap exception with a message, cause, and default error code.
     *
     * @param message the detailed description
     * @param cause   the underlying exception
     */
    public VstmRemapException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = DEFAULT_ERROR_CODE;
    }

    /**
     * Constructs a remap exception with a specific error code, message, and cause.
     *
     * @param errorCode the catalog error code
     * @param message   the detailed description
     * @param cause     the underlying exception
     */
    public VstmRemapException(String errorCode, String message, Throwable cause) {
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
