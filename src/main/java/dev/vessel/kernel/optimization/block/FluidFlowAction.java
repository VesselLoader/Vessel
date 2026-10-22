package dev.vessel.kernel.optimization.block;

import java.util.Objects;

/**
 * Immutable descriptor for a coalesced fluid state change action generated during BFS simulation.
 *
 * @param x block coordinate X
 * @param y block coordinate Y
 * @param z block coordinate Z
 * @param level fluid level (0 = source block, 1..7 = flowing level)
 * @param isFalling true if fluid is in free-fall downwards
 * @param fluidType simulation type
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record FluidFlowAction(
        int x,
        int y,
        int z,
        int level,
        boolean isFalling,
        FluidSimulationType fluidType
) {
    public FluidFlowAction {
        Objects.requireNonNull(fluidType, "fluidType cannot be null");
    }

    /**
     * Packs coordinates into a 64-bit primitive key.
     */
    public long packedPos() {
        return (((long) x & 0x3FFFFFF) << 38) | (((long) y & 0xFFF) << 26) | ((long) z & 0x3FFFFFF);
    }
}
