package dev.vessel.kernel.optimization.physics;

/**
 * Immutable report detailing the outcome of an early-exit 3D DDA ray-trace traversal.
 *
 * @param hasHit         true if ray was obstructed by a solid voxel before reaching maximum distance
 * @param hitX           grid X coordinate of the obstructing voxel
 * @param hitY           grid Y coordinate of the obstructing voxel
 * @param hitZ           grid Z coordinate of the obstructing voxel
 * @param hitDistance    traversal distance along the ray at which the obstruction was encountered
 * @param stepsEvaluated total number of discrete voxel boundary steps computed
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record RaytraceDdaResult(
        boolean hasHit,
        int hitX,
        int hitY,
        int hitZ,
        double hitDistance,
        int stepsEvaluated
) {
    /**
     * Creates an obstructed result snapshot.
     *
     * @param x        obstructing voxel X
     * @param y        obstructing voxel Y
     * @param z        obstructing voxel Z
     * @param distance distance traversed
     * @param steps    voxel steps evaluated
     * @return obstructed raytrace result
     */
    public static RaytraceDdaResult obstructed(int x, int y, int z, double distance, int steps) {
        return new RaytraceDdaResult(true, x, y, z, distance, steps);
    }

    /**
     * Creates an unobstructed (clear line of sight) result snapshot.
     *
     * @param steps    voxel steps evaluated
     * @param distance total traversed distance
     * @return unobstructed raytrace result
     */
    public static RaytraceDdaResult clear(int steps, double distance) {
        return new RaytraceDdaResult(false, 0, 0, 0, distance, steps);
    }
}
