package dev.vessel.kernel.gpu;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Global sovereign compute orchestrator routing high-throughput parallel Minecraft server workloads
 * across detected discrete GPUs, integrated GPUs, and CPU SIMD vector units.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class GpuComputeDispatcher {

    private static final AtomicReference<GpuComputeDispatcher> ACTIVE_DISPATCHER = new AtomicReference<>();

    private final GpuDeviceTopology topology;
    private final GpuBatchExplosionRaycastProcessor explosionProcessor;
    private final GpuBatchFluidSpreadProcessor fluidProcessor;

    public GpuComputeDispatcher() {
        this(new GpuHardwareDetector().detectHostTopology());
    }

    public GpuComputeDispatcher(GpuDeviceTopology topology) {
        this.topology = Objects.requireNonNull(topology, "topology cannot be null");
        this.explosionProcessor = new GpuBatchExplosionRaycastProcessor(topology);
        this.fluidProcessor = new GpuBatchFluidSpreadProcessor(topology);
    }

    /**
     * Obtains or lazily initializes the globally active compute dispatcher.
     *
     * @return active GpuComputeDispatcher singleton instance
     */
    public static GpuComputeDispatcher getActiveDispatcher() {
        GpuComputeDispatcher instance = ACTIVE_DISPATCHER.get();
        if (instance == null) {
            GpuComputeDispatcher created = new GpuComputeDispatcher();
            if (ACTIVE_DISPATCHER.compareAndSet(null, created)) {
                return created;
            }
            return ACTIVE_DISPATCHER.get();
        }
        return instance;
    }

    /**
     * Binds an explicit dispatcher instance.
     *
     * @param dispatcher explicit dispatcher instance
     */
    public static void setActiveDispatcher(GpuComputeDispatcher dispatcher) {
        ACTIVE_DISPATCHER.set(dispatcher);
    }

    public GpuDeviceTopology getTopology() {
        return topology;
    }

    public GpuDeviceTopology getHardwareTopology() {
        return topology;
    }

    public GpuExecutionStrategy getStrategy() {
        return topology.getResolvedStrategy();
    }

    public GpuBatchExplosionRaycastProcessor getExplosionProcessor() {
        return explosionProcessor;
    }

    public GpuBatchFluidSpreadProcessor getFluidProcessor() {
        return fluidProcessor;
    }

    /**
     * Dispatches a batch of explosion rays to the optimal hardware engine.
     */
    public int dispatchExplosionRaycasts(
            float originX, float originY, float originZ,
            float initialPower,
            float[] dirX, float[] dirY, float[] dirZ,
            int stepCount,
            float[] outDensities
    ) {
        return explosionProcessor.processRaycastBatch(
                originX, originY, originZ,
                initialPower,
                dirX, dirY, dirZ,
                stepCount,
                outDensities
        );
    }

    /**
     * Dispatches fluid cellular automata updates to the optimal hardware engine.
     */
    public int dispatchFluidSpread(
            byte[] fluidLevels,
            long[] passableMask,
            byte[] outNextLevels
    ) {
        return fluidProcessor.simulateChunkColumnFluid(
                fluidLevels,
                passableMask,
                outNextLevels
        );
    }

    /**
     * Dispatches spherical explosion raycasting and returns packed 64-bit coordinates of affected blocks.
     *
     * @param originX  explosion epicenter X
     * @param originY  explosion epicenter Y
     * @param originZ  explosion epicenter Z
     * @param power    explosion force magnitude
     * @param rayCount total rays to cast
     * @return array of packed 64-bit affected block positions
     */
    public long[] dispatchExplosionRaycasts(
            double originX, double originY, double originZ, float power, int rayCount
    ) {
        int count = Math.max(1, rayCount);
        float[] dirX = new float[count];
        float[] dirY = new float[count];
        float[] dirZ = new float[count];
        float[] outDensities = new float[count];

        double goldenRatio = (1.0 + Math.sqrt(5.0)) / 2.0;
        for (int i = 0; i < count; i++) {
            double theta = 2.0 * Math.PI * i / goldenRatio;
            double phi = Math.acos(1.0 - 2.0 * (i + 0.5) / count);
            dirX[i] = (float) (Math.cos(theta) * Math.sin(phi));
            dirY[i] = (float) Math.cos(phi);
            dirZ[i] = (float) (Math.sin(theta) * Math.sin(phi));
        }

        int stepCount = Math.max(1, (int) Math.ceil(power * 16.0f));
        dispatchExplosionRaycasts(
                (float) originX, (float) originY, (float) originZ,
                power,
                dirX, dirY, dirZ,
                stepCount,
                outDensities
        );

        long[] result = new long[count];
        for (int i = 0; i < count; i++) {
            int bx = (int) Math.floor(originX + dirX[i] * power);
            int by = (int) Math.floor(originY + dirY[i] * power);
            int bz = (int) Math.floor(originZ + dirZ[i] * power);
            result[i] = (((long) bx & 0x3FFFFFF) << 38) | (((long) by & 0xFFF) << 26) | ((long) bz & 0x3FFFFFF);
        }
        return result;
    }

    /**
     * Dispatches iterative fluid cellular automata propagation across the grid.
     *
     * @param initialGrid input 1D grid representation of fluid depths
     * @param width       grid width
     * @param height      grid height
     * @param iterations  simulation steps
     * @return updated fluid spread buffer
     */
    public byte[] dispatchFluidSpread(byte[] initialGrid, int width, int height, int iterations) {
        Objects.requireNonNull(initialGrid, "initialGrid cannot be null");
        byte[] current = initialGrid.clone();
        byte[] next = new byte[current.length];
        long[] passableMask = new long[(current.length + 63) / 64];
        java.util.Arrays.fill(passableMask, -1L);

        int steps = Math.max(1, iterations);
        for (int step = 0; step < steps; step++) {
            dispatchFluidSpread(current, passableMask, next);
            byte[] tmp = current;
            current = next;
            next = tmp;
        }
        return current;
    }
}
