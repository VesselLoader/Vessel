package dev.vessel.kernel.optimization.leak;

/**
 * Immutable statistics snapshot for the native buffer and primitive array recycler pool.
 *
 * @param totalAcquires cumulative acquire requests
 * @param totalReleases cumulative release recycling calls
 * @param cacheHits requests fulfilled from pre-allocated recycled pools
 * @param activePooledBuffers current count of idle buffers held in the recycler
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record PooledBufferStatistics(
        long totalAcquires,
        long totalReleases,
        long cacheHits,
        long activePooledBuffers
) {
    /**
     * Cache hit ratio between 0.0 and 1.0.
     *
     * @return hit ratio
     */
    public double hitRate() {
        return totalAcquires > 0 ? (double) cacheHits / totalAcquires : 0.0;
    }
}
