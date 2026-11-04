package dev.vessel.kernel.metrics;

import java.time.Instant;
import java.util.List;

/**
 * Generates honest runtime diagnostic dumps capturing active threads, JVM runtime version,
 * and memory metrics without fabricated status claims.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselDiagnosticDumpExporter {

    public VesselDiagnosticDumpExporter() {
    }

    /**
     * Formats a single-line diagnostic summary for {@code /vessel dump}.
     *
     * @return single-line diagnostic output
     */
    public String formatSingleLineDump() {
        int threads = Thread.activeCount();
        Runtime rt = Runtime.getRuntime();
        long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L);
        long maxMb = rt.maxMemory() / (1024L * 1024L);
        return "[VESSEL] Diagnostic dump: " + threads + " threads | " + usedMb + "MB/" + maxMb
                + "MB heap | " + rt.availableProcessors() + " cores";
    }

    /**
     * Formats multi-line console diagnostic dump for {@code vessel dump}.
     *
     * @param activeMinecraftVersion active Minecraft version string
     * @return list of console output lines
     */
    public List<String> formatConsoleDumpReport(String activeMinecraftVersion) {
        String mcVer = (activeMinecraftVersion != null && !activeMinecraftVersion.isBlank())
                ? activeMinecraftVersion
                : "unknown";
        int threads = Thread.activeCount();
        Runtime rt = Runtime.getRuntime();
        long totalMb = rt.totalMemory() / (1024L * 1024L);
        long freeMb = rt.freeMemory() / (1024L * 1024L);
        long usedMb = totalMb - freeMb;
        long maxMb = rt.maxMemory() / (1024L * 1024L);

        return List.of(
                "[VESSEL/DUMP] Snapshot captured at " + Instant.now(),
                "[VESSEL/DUMP] Runtime Telemetry: Java " + Runtime.version().feature()
                        + " (" + System.getProperty("java.vm.name", "OpenJDK") + ") | MC " + mcVer
                        + " | " + threads + " active JVM threads | " + rt.availableProcessors() + " cores",
                "[VESSEL/DUMP] Memory Profile   : " + usedMb + " MB used / " + totalMb
                        + " MB allocated (Max: " + maxMb + " MB, Free: " + freeMb + " MB)");
    }
}
