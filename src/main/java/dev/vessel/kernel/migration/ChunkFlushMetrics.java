package dev.vessel.kernel.migration;

/**
 * Immutable metrics snapshot recording asynchronous chunk flush pipeline performance.
 *
 * @param queuedCount        total number of chunk snapshots submitted
 * @param writtenCount       total number of chunk snapshots successfully written to disk
 * @param deduplicatedCount  total number of redundant snapshots merged or dropped via throttling
 * @param failureCount       total number of disk write exceptions encountered
 * @param currentQueueSize   instantaneous pending snapshots in the flush queue
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record ChunkFlushMetrics(
        long queuedCount,
        long writtenCount,
        long deduplicatedCount,
        long failureCount,
        int currentQueueSize
) {
}
