package dev.vessel.kernel.optimization.physics;

/**
 * Immutable outcome of a 3D Digital Differential Analyzer (DDA) ray-trace collision query.
 *
 * @param isHit true if ray intercepted a solid voxel
 * @param blockX integer voxel coordinate X
 * @param blockY integer voxel coordinate Y
 * @param blockZ integer voxel coordinate Z
 * @param hitFace normal direction index of the impacted voxel face (0:-Y, 1:+Y, 2:-Z, 3:+Z, 4:-X, 5:+X)
 * @param distance euclidean distance traversed along ray before impact
 * @param hitX exact impact point coordinate X
 * @param hitY exact impact point coordinate Y
 * @param hitZ exact impact point coordinate Z
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record FastRayTraceHit(
        boolean isHit,
        int blockX,
        int blockY,
        int blockZ,
        int hitFace,
        double distance,
        double hitX,
        double hitY,
        double hitZ
) {
    public static final FastRayTraceHit MISS = new FastRayTraceHit(false, 0, 0, 0, -1, Double.POSITIVE_INFINITY, 0.0, 0.0, 0.0);

    /**
     * Creates a hit result.
     */
    public static FastRayTraceHit hit(int bx, int by, int bz, int face, double dist, double hx, double hy, double hz) {
        return new FastRayTraceHit(true, bx, by, bz, face, dist, hx, hy, hz);
    }
}
