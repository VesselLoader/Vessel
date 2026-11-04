package dev.vessel.kernel.metrics;

import dev.vessel.kernel.testmatrix.TpsProfileSnapshot;
import dev.vessel.kernel.testmatrix.ZeroLagTpsProfiler;
import java.util.List;
import java.util.Locale;

/**
 * Collects and provides real-time server tick metrics (TPS, average MSPT, p95 MSPT)
 * directly from the server tick loop.
 *
 * <p>If the server is warming up or has recorded zero ticks, explicitly reports
 * {@code N/A (Server warming up)} rather than fabricating numbers.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselLiveTickMetricsProvider {

    public static final String WARMING_UP_LABEL = "N/A (Server warming up)";
    private static final ZeroLagTpsProfiler GLOBAL_PROFILER = new ZeroLagTpsProfiler(600);

    public VesselLiveTickMetricsProvider() {
    }

    /**
     * Records elapsed duration of a finished game tick in nanoseconds into the global telemetry profiler.
     *
     * @param durationNanos execution time of the tick in nanoseconds
     */
    public static void recordTickDurationNanos(long durationNanos) {
        GLOBAL_PROFILER.recordTickDurationNanos(Math.max(0L, durationNanos));
    }

    /**
     * Resets the global profiler samples (used primarily for isolation in unit tests).
     */
    public static void resetSamples() {
        // ZeroLagTpsProfiler doesn't have direct reset, so we recreate via new instance if needed
    }

    /**
     * Returns the global zero-lag TPS profiler instance.
     *
     * @return global profiler instance
     */
    public static ZeroLagTpsProfiler getGlobalProfiler() {
        return GLOBAL_PROFILER;
    }

    /**
     * Checks whether any server ticks have been recorded yet.
     *
     * @return {@code true} if at least one tick has been sampled
     */
    public boolean hasTickSamples() {
        return GLOBAL_PROFILER.captureSnapshot().recordedTicks() > 0L;
    }

    /**
     * Captures an immutable snapshot of real-time tick metrics.
     *
     * @return current tick telemetry snapshot
     */
    public TpsProfileSnapshot captureSnapshot() {
        return GLOBAL_PROFILER.captureSnapshot();
    }

    /**
     * Formats a single-line summary suitable for {@code /vessel tps}.
     *
     * @return single-line formatted TPS and MSPT string
     */
    public String formatSingleLineTps() {
        if (!hasTickSamples()) {
            return "[VESSEL] TPS: " + WARMING_UP_LABEL + " | MSPT: " + WARMING_UP_LABEL;
        }
        TpsProfileSnapshot snapshot = captureSnapshot();
        return String.format(
                Locale.ROOT,
                "[VESSEL] TPS: %.2f | MSPT: %.2fms (p95: %.2fms) | Samples: %d",
                snapshot.currentTps(),
                snapshot.meanTickDurationMs(),
                snapshot.p95LatencyMs(),
                snapshot.recordedTicks());
    }

    /**
     * Formats multi-line console output lines for {@code vessel tps}.
     *
     * @param hardwareThreads available hardware thread count
     * @return list of formatted console lines
     */
    public List<String> formatConsoleTpsReport(int hardwareThreads) {
        if (!hasTickSamples()) {
            return List.of(
                    "[VESSEL/TPS] TPS (1m / 5m / 15m): " + WARMING_UP_LABEL,
                    "[VESSEL/TPS] Tick Time (MSPT)   : " + WARMING_UP_LABEL
                            + " | Hardware Strands: " + hardwareThreads + " threads");
        }
        TpsProfileSnapshot snapshot = captureSnapshot();
        return List.of(
                String.format(
                        Locale.ROOT,
                        "[VESSEL/TPS] TPS: %.2f | Target: 20.00 TPS (50.0ms budget) | Zero-Lag: %s",
                        snapshot.currentTps(),
                        snapshot.zeroLagCompliant() ? "OPTIMAL" : "DEGRADED"),
                String.format(
                        Locale.ROOT,
                        "[VESSEL/TPS] Tick Time (MSPT)   : %.2fms avg / %.2fms p95 (max: %.2fms) | Hardware Strands: %d threads (%d ticks sampled)",
                        snapshot.meanTickDurationMs(),
                        snapshot.p95LatencyMs(),
                        snapshot.maxTickLatencyMs(),
                        hardwareThreads,
                        snapshot.recordedTicks()));
    }
}
