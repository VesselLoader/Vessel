package dev.vessel.kernel.adapter.forge;

import java.util.Objects;

/**
 * Immutable carrier record for FML lifecycle events dispatched across Forge mod containers.
 *
 * @param phase          the target lifecycle phase
 * @param modId          the recipient mod identifier
 * @param timestampNanos monotonic timestamp in nanoseconds when this event was synthesized
 * @param contextPayload optional arbitrary context payload passed to the mod handler
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record FmlLifecycleEvent(
        FmlLifecyclePhase phase,
        String modId,
        long timestampNanos,
        Object contextPayload
) {

    /**
     * Compact constructor validating non-null mandatory fields.
     */
    public FmlLifecycleEvent {
        Objects.requireNonNull(phase, "phase must not be null");
        Objects.requireNonNull(modId, "modId must not be null");
    }
}
