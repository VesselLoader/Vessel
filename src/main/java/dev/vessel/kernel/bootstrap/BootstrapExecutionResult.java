package dev.vessel.kernel.bootstrap;

import java.util.Objects;

/**
 * Immutable audit record capturing the outcome of an individual bootstrap phase.
 *
 * @param phase the executed bootstrap phase
 * @param success whether the phase succeeded without exceptions
 * @param durationNanos elapsed execution time in nanoseconds
 * @param errorMessage descriptive failure message if unsuccessful, or empty string
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record BootstrapExecutionResult(
        BootstrapPhase phase,
        boolean success,
        long durationNanos,
        String errorMessage
) {
    public BootstrapExecutionResult {
        Objects.requireNonNull(phase, "phase cannot be null");
        errorMessage = (errorMessage != null) ? errorMessage : "";
    }
}
