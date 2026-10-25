package dev.vessel.kernel.testmatrix;

import java.util.Objects;

/**
 * Immutable statistics describing hardware memory synchronization performance and throughput.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param operationsCount total barrier operations executed
 * @param elapsedNanos total duration in nanoseconds
 * @param opsPerSecond calculated operations per second throughput
 * @param mode memory ordering mode tested
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record MemoryBarrierStats(
        long operationsCount,
        long elapsedNanos,
        double opsPerSecond,
        MemoryBarrierMode mode
) {
    public MemoryBarrierStats {
        Objects.requireNonNull(mode, "mode must not be null");
    }
}
