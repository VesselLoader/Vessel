package dev.vessel.kernel.gpu;

/**
 * Categorization of physical and logical graphics/compute processing units detected on the server host.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum GpuDeviceKind {

    /**
     * Dedicated Discrete GPU (e.g. NVIDIA RTX/Tesla, AMD Radeon RX/Pro, Intel Arc) with dedicated VRAM.
     * Optimal for high-throughput batch raycasting, complex matrix multiplication, and massive explosion physics.
     */
    DISCRETE,

    /**
     * Integrated GPU (e.g. Intel UHD/Iris Xe, AMD Radeon Vega/RDNA 680M/780M, Apple Silicon) sharing system RAM.
     * Optimal for zero-PCIe-transfer cellular automata, fluid flow, and memory-bandwidth-bound spatial indexing.
     */
    INTEGRATED,

    /**
     * Software SIMD Vector fallback (AVX2 / AVX-512 / ARM Neon) when no physical GPU is present.
     */
    SOFTWARE_FALLBACK
}
