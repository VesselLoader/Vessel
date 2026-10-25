package dev.vessel.kernel.optimization.physics;

import java.util.Objects;

/**
 * Immutable execution telemetry report resulting from an Alternate Current BFS network evaluation.
 *
 * @param nodesEvaluated       total redstone wire nodes identified in the network
 * @param wiresUpdated         count of wire blocks whose power state was modified
 * @param maxSignalDiscovered  highest signal intensity detected across all network nodes
 * @param executionTimeNanos   nanosecond duration taken to compute the network
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record RedstoneSolverResult(
        int nodesEvaluated,
        int wiresUpdated,
        int maxSignalDiscovered,
        long executionTimeNanos
) {
    /**
     * Constructs and validates a RedstoneSolverResult.
     */
    public RedstoneSolverResult {
        if (nodesEvaluated < 0) {
            throw new IllegalArgumentException("nodesEvaluated cannot be negative: " + nodesEvaluated);
        }
        if (wiresUpdated < 0) {
            throw new IllegalArgumentException("wiresUpdated cannot be negative: " + wiresUpdated);
        }
        if (maxSignalDiscovered < 0 || maxSignalDiscovered > 15) {
            throw new IllegalArgumentException("maxSignalDiscovered must be in [0, 15]: " + maxSignalDiscovered);
        }
    }
}
