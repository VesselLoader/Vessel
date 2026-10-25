package dev.vessel.kernel.optimization.physics;

/**
 * Immutable telemetry statistics describing the operational efficiency of a {@link VoxelShapeAabbCachePool}.
 *
 * @param poolCapacity   total maximum capacity of the pre-allocated pool
 * @param availableCount current number of idle instances available for immediate reuse
 * @param totalAcquired  cumulative count of acquire invocations
 * @param totalReleased  cumulative count of release invocations
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record VoxelAabbPoolStats(
        int poolCapacity,
        int availableCount,
        long totalAcquired,
        long totalReleased
) {
    /**
     * Validates pool statistics.
     */
    public VoxelAabbPoolStats {
        if (poolCapacity < 0) {
            throw new IllegalArgumentException("poolCapacity cannot be negative: " + poolCapacity);
        }
        if (availableCount < 0) {
            throw new IllegalArgumentException("availableCount cannot be negative: " + availableCount);
        }
        if (totalAcquired < 0) {
            throw new IllegalArgumentException("totalAcquired cannot be negative: " + totalAcquired);
        }
        if (totalReleased < 0) {
            throw new IllegalArgumentException("totalReleased cannot be negative: " + totalReleased);
        }
    }
}
