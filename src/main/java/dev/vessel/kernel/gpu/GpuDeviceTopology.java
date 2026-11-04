package dev.vessel.kernel.gpu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Encapsulates the complete compute device inventory on the server host and computes the optimal execution strategy.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class GpuDeviceTopology {

    private final List<GpuDeviceInfo> discreteGpus;
    private final List<GpuDeviceInfo> integratedGpus;
    private final GpuDeviceInfo fallbackDevice;
    private final GpuExecutionStrategy resolvedStrategy;

    public GpuDeviceTopology(List<GpuDeviceInfo> devices) {
        Objects.requireNonNull(devices, "devices cannot be null");
        List<GpuDeviceInfo> dList = new ArrayList<>();
        List<GpuDeviceInfo> iList = new ArrayList<>();
        GpuDeviceInfo fallback = null;

        for (GpuDeviceInfo info : devices) {
            if (info.kind() == GpuDeviceKind.DISCRETE) {
                dList.add(info);
            } else if (info.kind() == GpuDeviceKind.INTEGRATED) {
                iList.add(info);
            } else if (info.kind() == GpuDeviceKind.SOFTWARE_FALLBACK) {
                fallback = info;
            }
        }

        this.discreteGpus = Collections.unmodifiableList(dList);
        this.integratedGpus = Collections.unmodifiableList(iList);
        this.fallbackDevice = (fallback != null)
                ? fallback
                : GpuDeviceInfo.createSoftwareFallback(Runtime.getRuntime().availableProcessors());

        // Determine optimal strategy based on physical topology
        if (discreteGpus.size() >= 2) {
            this.resolvedStrategy = GpuExecutionStrategy.MULTI_DGPU_STRIPED;
        } else if (!discreteGpus.isEmpty() && !integratedGpus.isEmpty()) {
            this.resolvedStrategy = GpuExecutionStrategy.HYBRID_ASYMMETRIC;
        } else if (!discreteGpus.isEmpty()) {
            this.resolvedStrategy = GpuExecutionStrategy.DISCRETE_ONLY;
        } else if (!integratedGpus.isEmpty()) {
            this.resolvedStrategy = GpuExecutionStrategy.INTEGRATED_ONLY;
        } else {
            this.resolvedStrategy = GpuExecutionStrategy.CPU_SIMD_FALLBACK;
        }
    }

    public List<GpuDeviceInfo> getDiscreteGpus() {
        return discreteGpus;
    }

    public List<GpuDeviceInfo> getIntegratedGpus() {
        return integratedGpus;
    }

    public GpuDeviceInfo getFallbackDevice() {
        return fallbackDevice;
    }

    public GpuExecutionStrategy getResolvedStrategy() {
        return resolvedStrategy;
    }

    public boolean hasHardwareGpu() {
        return !discreteGpus.isEmpty() || !integratedGpus.isEmpty();
    }

    public int totalHardwareGpuCount() {
        return discreteGpus.size() + integratedGpus.size();
    }
}
