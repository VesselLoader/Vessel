package dev.vessel.kernel.testmatrix;

/**
 * Immutable report generated after executing an extreme concurrent virtual bot stress load test.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param totalBots number of concurrent virtual player bots simulated
 * @param totalPacketsSent total packets generated and transmitted by all bots
 * @param totalPacketsProcessed total packets ingested and processed by the server pipeline
 * @param packetsDropped total packets dropped due to queue congestion or backpressure
 * @param averageLatencyMs average latency per packet batch in milliseconds
 * @param errorCount number of unhandled exceptions or protocol errors encountered
 * @param targetTps target ticks per second expected
 * @param achievedTps actual ticks per second achieved during stress test
 * @param durationMs total stress test execution duration in milliseconds
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record ModStressLoadReport(
        int totalBots,
        long totalPacketsSent,
        long totalPacketsProcessed,
        long packetsDropped,
        double averageLatencyMs,
        int errorCount,
        double targetTps,
        double achievedTps,
        long durationMs
) {
    public boolean isStabilityCompliant() {
        return errorCount == 0 && achievedTps >= 19.95 && packetsDropped == 0;
    }
}
