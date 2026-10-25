package dev.vessel.kernel.tick;

/**
 * Functional callback determining whether a 3D block coordinate is passable during pathfinding.
 * <p>
 * Directly resolves ERR-40 and OPT-040..050.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
@FunctionalInterface
public interface PathPassabilityPredicate {

    /**
     * Evaluates whether an entity can traverse into the specified world coordinates.
     *
     * @param x world X coordinate
     * @param y world Y coordinate
     * @param z world Z coordinate
     * @return {@code true} if traversable/passable; {@code false} if solid obstacle
     */
    boolean isPassable(int x, int y, int z);
}
