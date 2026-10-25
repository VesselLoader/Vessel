package dev.vessel.kernel.network.protocol;

/**
 * Immutable telemetry metrics tracking zero-copy slice operations and heap allocation savings.
 *
 * @param sliceOperationsTotal count of zero-copy slice operations executed
 * @param totalBytesSliced     cumulative number of bytes sliced via zero-copy windows
 * @param heapAllocationsSaved count of defensive heap byte array allocations completely eliminated
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record ByteBufSliceMetrics(
        long sliceOperationsTotal,
        long totalBytesSliced,
        long heapAllocationsSaved
) {

    /**
     * Compact constructor validating non-negative constraints.
     */
    public ByteBufSliceMetrics {
        if (sliceOperationsTotal < 0) {
            throw new IllegalArgumentException("sliceOperationsTotal cannot be negative: " + sliceOperationsTotal);
        }
        if (totalBytesSliced < 0) {
            throw new IllegalArgumentException("totalBytesSliced cannot be negative: " + totalBytesSliced);
        }
        if (heapAllocationsSaved < 0) {
            throw new IllegalArgumentException("heapAllocationsSaved cannot be negative: " + heapAllocationsSaved);
        }
    }
}
