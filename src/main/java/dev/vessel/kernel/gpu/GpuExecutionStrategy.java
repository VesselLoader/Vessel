package dev.vessel.kernel.gpu;

/**
 * Execution dispatch strategy determined from server host GPU hardware topology.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum GpuExecutionStrategy {

    /**
     * Server has exactly one dedicated dGPU: offload large batches (N &gt;= 256) to dedicated VRAM.
     */
    DISCRETE_ONLY,

    /**
     * Server has only an iGPU (Unified Memory): offload memory-bound cellular automata and fluid flow with zero PCIe copy.
     */
    INTEGRATED_ONLY,

    /**
     * Server has BOTH an iGPU and a dGPU:
     * - iGPU handles fluid spread &amp; spatial index updates (Zero-copy unified memory).
     * - dGPU handles heavy explosion raycasts &amp; multi-block entity collision sweeps.
     */
    HYBRID_ASYMMETRIC,

    /**
     * Server has 2 or more discrete GPUs: batches are striped across all available dGPUs via lock-free round robin.
     */
    MULTI_DGPU_STRIPED,

    /**
     * Server is headless or lacks GPU drivers: executes via 8-lane/16-lane CPU SIMD vector kernels.
     */
    CPU_SIMD_FALLBACK
}
