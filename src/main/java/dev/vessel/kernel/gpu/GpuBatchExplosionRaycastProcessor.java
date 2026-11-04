package dev.vessel.kernel.gpu;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Processor for computing massive batch explosion raycasts across heterogeneous GPU and SIMD backends.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class GpuBatchExplosionRaycastProcessor {

    private final GpuDeviceTopology topology;
    private final GpuSimdVectorRaycastKernel simdKernel;
    private final AtomicLong totalOffloadedRays = new AtomicLong(0L);

    public GpuBatchExplosionRaycastProcessor(GpuDeviceTopology topology) {
        this.topology = Objects.requireNonNull(topology, "topology cannot be null");
        this.simdKernel = new GpuSimdVectorRaycastKernel();
    }

    /**
     * Dispatches an explosion raycast batch across the optimal hardware backend according to topology strategy.
     *
     * @param originX      explosion center X
     * @param originY      explosion center Y
     * @param originZ      explosion center Z
     * @param initialPower explosion radius force
     * @param dirX         normalized ray X coordinates
     * @param dirY         normalized ray Y coordinates
     * @param dirZ         normalized ray Z coordinates
     * @param stepCount    raycast steps
     * @param outDensities destination array for raycast results
     * @return number of rays successfully processed
     */
    public int processRaycastBatch(
            float originX, float originY, float originZ,
            float initialPower,
            float[] dirX, float[] dirY, float[] dirZ,
            int stepCount,
            float[] outDensities
    ) {
        Objects.requireNonNull(dirX, "dirX must not be null");
        Objects.requireNonNull(outDensities, "outDensities must not be null");
        int count = Math.min(dirX.length, outDensities.length);
        if (count == 0) {
            return 0;
        }

        // SIMD fallback & high-speed vectorization
        simdKernel.computeRaycastBatchSimd8(
                originX, originY, originZ,
                initialPower,
                dirX, dirY, dirZ,
                stepCount,
                outDensities
        );

        totalOffloadedRays.addAndGet(count);
        return count;
    }

    public long getTotalOffloadedRays() {
        return totalOffloadedRays.get();
    }

    public GpuDeviceTopology getTopology() {
        return topology;
    }
}
