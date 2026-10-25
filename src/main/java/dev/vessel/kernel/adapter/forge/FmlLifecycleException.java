package dev.vessel.kernel.adapter.forge;

import java.util.Objects;

/**
 * Typed exception thrown upon lifecycle transition failures during Forge mod initialization.
 * <p>
 * Eliminates ERR-61 and addresses ecosystem failure codes ECO-001 through ECO-010.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class FmlLifecycleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final String modId;
    private final FmlLifecyclePhase phase;

    /**
     * Constructs a lifecycle exception with an explicit error code, modId, phase, message and cause.
     *
     * @param errorCode canonical error code (e.g. ERR-61 or ECO-001..010)
     * @param modId     the failing mod namespace identifier
     * @param phase     the lifecycle phase when the failure occurred
     * @param message   descriptive message
     * @param cause     underlying throwable cause
     */
    public FmlLifecycleException(
            String errorCode,
            String modId,
            FmlLifecyclePhase phase,
            String message,
            Throwable cause
    ) {
        super(String.format("[%s] Mod '%s' failed during %s: %s", errorCode, modId, phase, message), cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
        this.modId = Objects.requireNonNull(modId, "modId must not be null");
        this.phase = Objects.requireNonNull(phase, "phase must not be null");
    }

    /**
     * Returns the canonical error code.
     *
     * @return error code string
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Returns the target mod identifier.
     *
     * @return mod identifier
     */
    public String getModId() {
        return modId;
    }

    /**
     * Returns the lifecycle phase.
     *
     * @return lifecycle phase
     */
    public FmlLifecyclePhase getPhase() {
        return phase;
    }
}
