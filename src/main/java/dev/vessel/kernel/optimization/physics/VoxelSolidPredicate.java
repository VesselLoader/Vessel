package dev.vessel.kernel.optimization.physics;

/**
 * Functional predicate evaluating whether a 3D block coordinate is solid/collidable
 * with 0 heap allocation during ray-trace collision testing.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
@FunctionalInterface
public interface VoxelSolidPredicate {

    /**
     * Checks if voxel at block coordinates (x, y, z) blocks collision or line-of-sight.
     *
     * @param x voxel coordinate X
     * @param y voxel coordinate Y
     * @param z voxel coordinate Z
     * @return true if solid
     */
    boolean isSolid(int x, int y, int z);
}
