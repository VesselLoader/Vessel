package dev.vessel.kernel.gpu;

import java.util.Objects;

/**
 * Immutable descriptor capturing hardware capability, topology, and memory metrics of a compute device.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @param deviceId      unique system device index
 * @param deviceName    human-readable hardware model (e.g. "NVIDIA GeForce RTX 4090", "Intel Iris Xe")
 * @param vendor        hardware vendor identifier
 * @param kind          device categorization (DISCRETE, INTEGRATED, SOFTWARE_FALLBACK)
 * @param vramBytes     dedicated or shared video memory in bytes
 * @param computeUnits  number of hardware compute units / streaming multiprocessors
 * @param isAvailable   whether the device is initialized and ready for computation
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record GpuDeviceInfo(
        int deviceId,
        String deviceName,
        String vendor,
        GpuDeviceKind kind,
        long vramBytes,
        int computeUnits,
        boolean isAvailable
) {

    public GpuDeviceInfo {
        Objects.requireNonNull(deviceName, "deviceName must not be null");
        Objects.requireNonNull(vendor, "vendor must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
    }

    /**
     * Creates a fallback software descriptor when host server has no GPU installed.
     *
     * @param cpuCores available host CPU core count
     * @return software fallback device descriptor
     */
    public static GpuDeviceInfo createSoftwareFallback(int cpuCores) {
        return new GpuDeviceInfo(
                -1,
                "CPU SIMD Vector Accelerator (AVX2/AVX-512/Neon)",
                "Host CPU",
                GpuDeviceKind.SOFTWARE_FALLBACK,
                Runtime.getRuntime().maxMemory(),
                Math.max(1, cpuCores),
                true
        );
    }
}
