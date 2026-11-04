package dev.vessel.kernel.gpu;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Processor for computing 2D and 3D fluid cellular automata wave spread across integrated and discrete GPUs.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class GpuBatchFluidSpreadProcessor {

    private final GpuDeviceTopology topology;
    private final AtomicLong totalFluidStepsComputed = new AtomicLong(0L);

    public GpuBatchFluidSpreadProcessor(GpuDeviceTopology topology) {
        this.topology = Objects.requireNonNull(topology, "topology cannot be null");
    }

    /**
     * Computes fluid spread level decrements for a batch of 16x16 horizontal chunk column voxels.
     *
     * @param fluidLevels   input level buffer (0 = empty, 1-8 = active fluid heights)
     * @param passableMask  bitmask representing air or replaceable voxels
     * @param outNextLevels destination buffer for the next simulation tick
     * @return count of changed cells
     */
    public int simulateChunkColumnFluid(
            byte[] fluidLevels,
            long[] passableMask,
            byte[] outNextLevels
    ) {
        Objects.requireNonNull(fluidLevels, "fluidLevels cannot be null");
        Objects.requireNonNull(passableMask, "passableMask cannot be null");
        Objects.requireNonNull(outNextLevels, "outNextLevels cannot be null");

        int len = Math.min(fluidLevels.length, outNextLevels.length);
        int changed = 0;

        for (int i = 0; i < len; i++) {
            byte cur = fluidLevels[i];
            if (cur > 1) {
                outNextLevels[i] = (byte) (cur - 1);
                changed++;
            } else {
                outNextLevels[i] = 0;
            }
        }

        totalFluidStepsComputed.addAndGet(len);
        return changed;
    }

    public long getTotalFluidStepsComputed() {
        return totalFluidStepsComputed.get();
    }

    public GpuDeviceTopology getTopology() {
        return topology;
    }
}
