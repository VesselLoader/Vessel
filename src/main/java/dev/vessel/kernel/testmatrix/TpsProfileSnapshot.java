package dev.vessel.kernel.testmatrix;

/**
 * Immutable snapshot of server tick telemetry, micro-latencies, and TPS metrics.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param currentTps calculated ticks per second across the sample window
 * @param meanTickDurationMs arithmetic mean of tick duration in milliseconds
 * @param p95LatencyMs 95th percentile tick latency in milliseconds
 * @param p99LatencyMs 99th percentile tick latency in milliseconds
 * @param p999LatencyMs 99.9th percentile tick latency in milliseconds
 * @param maxTickLatencyMs worst observed tick latency spike in milliseconds
 * @param zeroLagCompliant true if TPS &gt;= 19.95 and p99 latency &lt; 50.0ms
 * @param recordedTicks total number of ticks sampled in this window
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record TpsProfileSnapshot(
        double currentTps,
        double meanTickDurationMs,
        double p95LatencyMs,
        double p99LatencyMs,
        double p999LatencyMs,
        double maxTickLatencyMs,
        boolean zeroLagCompliant,
        long recordedTicks
) {
}
