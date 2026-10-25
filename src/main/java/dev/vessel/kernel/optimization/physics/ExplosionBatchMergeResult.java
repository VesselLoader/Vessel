package dev.vessel.kernel.optimization.physics;

/**
 * Immutable telemetry outcome produced when committing a merged explosion batch.
 *
 * @param totalBlocksProcessed   number of unique block destructions applied
 * @param affectedSectionsCount  number of distinct 16x16x16 chunk sections modified
 * @param lightUpdatesDispatched number of unified section lighting updates dispatched
 * @param executionTimeNanos     duration of batch processing in nanoseconds
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record ExplosionBatchMergeResult(
        int totalBlocksProcessed,
        int affectedSectionsCount,
        int lightUpdatesDispatched,
        long executionTimeNanos
) {
    /**
     * Validates merge result invariants.
     */
    public ExplosionBatchMergeResult {
        if (totalBlocksProcessed < 0) {
            throw new IllegalArgumentException("totalBlocksProcessed cannot be negative: " + totalBlocksProcessed);
        }
        if (affectedSectionsCount < 0) {
            throw new IllegalArgumentException("affectedSectionsCount cannot be negative: " + affectedSectionsCount);
        }
        if (lightUpdatesDispatched < 0) {
            throw new IllegalArgumentException("lightUpdatesDispatched cannot be negative: " + lightUpdatesDispatched);
        }
    }
}
