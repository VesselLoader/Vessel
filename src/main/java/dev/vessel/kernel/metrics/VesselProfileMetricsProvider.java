package dev.vessel.kernel.metrics;

import dev.vessel.bootstrap.ecosystem.BuiltInGeyserAndSparkProvisioner;
import dev.vessel.command.BuiltinSamplerProfiler;
import dev.vessel.kernel.testmatrix.TpsProfileSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Manages the built-in sampler profiler lifecycle (start, stop, status) and telemetry
 * without hardcoding fake CPU or latency percentages.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselProfileMetricsProvider {

    public VesselProfileMetricsProvider() {
    }

    /**
     * Formats a single-line profile summary for {@code /vessel profile}.
     *
     * @param subArgs command arguments (e.g., start, stop, status)
     * @return single-line response
     */
    public String handleSingleLineCommand(String[] subArgs) {
        String action = (subArgs != null && subArgs.length > 1 && subArgs[1] != null)
                ? subArgs[1].trim().toLowerCase(Locale.ROOT)
                : "status";
        BuiltinSamplerProfiler profiler = BuiltinSamplerProfiler.getDefaultProfiler();
        return switch (action) {
            case "start" -> {
                profiler.startSamplingSession();
                yield "[VESSEL] Built-in Profiler: Sampling session STARTED";
            }
            case "stop" -> {
                Map<String, Long> summary = profiler.stopAndExportFlamegraphSummary();
                yield "[VESSEL] Built-in Profiler: STOPPED (" + summary.size() + " method frames exported)";
            }
            default -> {
                boolean active = profiler.isSamplingActive();
                yield "[VESSEL] Built-in Profiler: " + (active ? "ACTIVE (Sampling)" : "IDLE (Use /vessel profile start|stop)");
            }
        };
    }

    /**
     * Formats multi-line console output for {@code vessel profile}.
     *
     * @param subArgs command arguments
     * @param tickMetricsProvider provider for live tick metrics
     * @return list of console output lines
     */
    public List<String> handleConsoleCommand(String[] subArgs, VesselLiveTickMetricsProvider tickMetricsProvider) {
        String action = (subArgs != null && subArgs.length > 1 && subArgs[1] != null)
                ? subArgs[1].trim().toLowerCase(Locale.ROOT)
                : "status";
        BuiltinSamplerProfiler profiler = BuiltinSamplerProfiler.getDefaultProfiler();

        if ("start".equals(action)) {
            profiler.startSamplingSession();
            return List.of(
                    "[VESSEL/PROFILER] Built-in spark-style Sampler Profiler - Session STARTED",
                    "[VESSEL/PROFILER] Sampling active method frames across worker strands (run 'vessel profile stop' to view summary).");
        }
        if ("stop".equals(action)) {
            Map<String, Long> summary = profiler.stopAndExportFlamegraphSummary();
            List<String> lines = new ArrayList<>();
            lines.add("[VESSEL/PROFILER] Built-in spark-style Sampler Profiler - Session STOPPED");
            lines.add("[VESSEL/PROFILER] Captured " + summary.size() + " distinct method frame(s):");
            int count = 0;
            for (Map.Entry<String, Long> entry : summary.entrySet()) {
                if (count++ >= 5) {
                    break;
                }
                double ms = entry.getValue() / 1_000_000.0;
                lines.add(String.format(Locale.ROOT, "  - %s: %.2fms", entry.getKey(), ms));
            }
            if (summary.isEmpty()) {
                lines.add("  - (No method samples recorded during this session)");
            }
            return List.copyOf(lines);
        }

        // Action: status
        boolean active = profiler.isSamplingActive();
        int sampleCount = profiler.getSampleCount();
        String budgetUsage;
        if (tickMetricsProvider != null && tickMetricsProvider.hasTickSamples()) {
            TpsProfileSnapshot snap = tickMetricsProvider.captureSnapshot();
            double percent = (snap.meanTickDurationMs() / 50.0) * 100.0;
            budgetUsage = String.format(Locale.ROOT, "%.1f%% (MSPT: %.2fms / 50.0ms target)", percent, snap.meanTickDurationMs());
        } else {
            budgetUsage = VesselLiveTickMetricsProvider.WARMING_UP_LABEL;
        }

        return List.of(
                "[VESSEL/PROFILER] Built-in spark-style Sampler Profiler ("
                        + BuiltInGeyserAndSparkProvisioner.DEFAULT_LATEST_SPARK_VERSION + ") - Mode: "
                        + (active ? "ACTIVE" : "IDLE"),
                "[VESSEL/PROFILER] CPU Tick Budget Usage: " + budgetUsage + " | Tracked Frames: " + sampleCount);
    }
}
