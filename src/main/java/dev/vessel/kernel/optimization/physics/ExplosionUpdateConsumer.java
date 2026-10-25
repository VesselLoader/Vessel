package dev.vessel.kernel.optimization.physics;

/**
 * Consumer callback for applying batched explosion block state changes.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
@FunctionalInterface
public interface ExplosionUpdateConsumer {

    /**
     * Applies a block state update resulting from an explosion.
     *
     * @param x               block world X coordinate
     * @param y               block world Y coordinate
     * @param z               block world Z coordinate
     * @param newBlockStateId target block state ID (e.g. 0 for air)
     */
    void applyBlockChange(int x, int y, int z, int newBlockStateId);
}
