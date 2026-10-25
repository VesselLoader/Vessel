package dev.vessel.kernel.bootstrap;

import java.util.Objects;

/**
 * Domain-specific exception thrown when the VESSEL bootstrap sequence encounters an unrecoverable failure.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class VesselBootstrapException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final BootstrapPhase failedPhase;

    /**
     * Constructs a VesselBootstrapException with message and failed phase.
     *
     * @param message failure description
     * @param failedPhase the phase during which execution failed
     */
    public VesselBootstrapException(String message, BootstrapPhase failedPhase) {
        super(Objects.requireNonNull(message, "message cannot be null"));
        this.failedPhase = Objects.requireNonNull(failedPhase, "failedPhase cannot be null");
    }

    /**
     * Constructs a VesselBootstrapException with message, cause, and failed phase.
     *
     * @param message failure description
     * @param cause underlying cause
     * @param failedPhase the phase during which execution failed
     */
    public VesselBootstrapException(String message, Throwable cause, BootstrapPhase failedPhase) {
        super(Objects.requireNonNull(message, "message cannot be null"), cause);
        this.failedPhase = Objects.requireNonNull(failedPhase, "failedPhase cannot be null");
    }

    /**
     * Returns the bootstrap phase that experienced the failure.
     *
     * @return the failed phase
     */
    public BootstrapPhase failedPhase() {
        return failedPhase;
    }
}
