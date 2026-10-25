package dev.vessel.kernel.autoupdate;

/**
 * Specialized runtime exception thrown when a candidate patch sequence fails integrity or continuity
 * validation (e.g. revision gap, out-of-order patches, or checksum failure).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public final class PatchChainValidationException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@link PatchChainValidationException} with diagnostic message.
     *
     * @param message failure explanation
     */
    public PatchChainValidationException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@link PatchChainValidationException} with diagnostic message and cause.
     *
     * @param message failure explanation
     * @param cause   underlying exception
     */
    public PatchChainValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
