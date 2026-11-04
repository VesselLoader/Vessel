package dev.vessel.kernel.metrics;

import java.util.List;
import java.util.Locale;

/**
 * Inspects real JVM heap memory metrics directly from {@link Runtime#getRuntime()},
 * reporting exact used, free, total, and max megabytes with zero fabricated leak or world statistics.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselHeapMemoryInspector {

    private static final long BYTES_IN_MB = 1024L * 1024L;

    public VesselHeapMemoryInspector() {
    }

    public long getUsedMemoryBytes() {
        Runtime rt = Runtime.getRuntime();
        return Math.max(0L, rt.totalMemory() - rt.freeMemory());
    }

    public long getTotalMemoryBytes() {
        return Runtime.getRuntime().totalMemory();
    }

    public long getFreeMemoryBytes() {
        return Runtime.getRuntime().freeMemory();
    }

    public long getMaxMemoryBytes() {
        return Runtime.getRuntime().maxMemory();
    }

    public long getUsedMemoryMb() {
        return getUsedMemoryBytes() / BYTES_IN_MB;
    }

    public long getTotalMemoryMb() {
        return getTotalMemoryBytes() / BYTES_IN_MB;
    }

    public long getFreeMemoryMb() {
        return getFreeMemoryBytes() / BYTES_IN_MB;
    }

    public long getMaxMemoryMb() {
        return getMaxMemoryBytes() / BYTES_IN_MB;
    }

    /**
     * Formats a single-line summary for {@code /vessel heap}.
     *
     * @return single-line heap summary
     */
    public String formatSingleLineHeap() {
        return "[VESSEL] JVM Heap: " + getUsedMemoryMb() + " MB / " + getTotalMemoryMb()
                + " MB allocated (Max: " + getMaxMemoryMb() + " MB, Free: " + getFreeMemoryMb() + " MB)";
    }

    /**
     * Formats console lines for {@code vessel heap}.
     *
     * @return list of console output lines
     */
    public List<String> formatConsoleHeapReport() {
        long usedMb = getUsedMemoryMb();
        long totalMb = getTotalMemoryMb();
        long maxMb = getMaxMemoryMb();
        long freeMb = getFreeMemoryMb();
        double percentAllocated = (totalMb > 0) ? (usedMb * 100.0 / totalMb) : 0.0;
        double percentMax = (maxMb > 0) ? (usedMb * 100.0 / maxMb) : 0.0;
        return List.of(
                "[VESSEL/HEAP] JVM Memory : " + usedMb + " MB used / " + totalMb
                        + " MB allocated (Max: " + maxMb + " MB, Free: " + freeMb + " MB)",
                String.format(
                        Locale.ROOT,
                        "[VESSEL/HEAP] Heap Usage : %.1f%% of allocated memory, %.1f%% of maximum ceiling",
                        percentAllocated,
                        percentMax));
    }
}
