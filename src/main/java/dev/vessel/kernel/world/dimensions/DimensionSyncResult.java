package dev.vessel.kernel.world.dimensions;

import java.util.Objects;

/**
 * Immutable report detailing the outcome of a dynamic dimension lifecycle transition.
 *
 * @param dimensionKey   canonical resource location identifier of the dimension (e.g. "minecraft:the_nether", "mining_dim:overworld")
 * @param state          resulting state of the dimension after the operation
 * @param success        true if the state transition was executed without error
 * @param message        diagnostic status or error explanation
 * @param timestampNanos timestamp when the transition was finalized
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record DimensionSyncResult(
        String dimensionKey,
        DimensionLifecycleState state,
        boolean success,
        String message,
        long timestampNanos
) {
    /**
     * Validates record parameters.
     */
    public DimensionSyncResult {
        Objects.requireNonNull(dimensionKey, "dimensionKey cannot be null");
        Objects.requireNonNull(state, "state cannot be null");
        Objects.requireNonNull(message, "message cannot be null");
    }
}
