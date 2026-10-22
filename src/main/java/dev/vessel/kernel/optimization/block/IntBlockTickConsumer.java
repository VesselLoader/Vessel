package dev.vessel.kernel.optimization.block;

/**
 * Functional callback interface accepting primitive block tick details with 0 heap allocation.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
@FunctionalInterface
public interface IntBlockTickConsumer {

    /**
     * Consumes a triggered scheduled block tick.
     *
     * @param scheduledTick tick timestamp when this tick was scheduled
     * @param x block coordinate X
     * @param y block coordinate Y
     * @param z block coordinate Z
     * @param priority tick priority integer
     */
    void accept(long scheduledTick, int x, int y, int z, int priority);
}
