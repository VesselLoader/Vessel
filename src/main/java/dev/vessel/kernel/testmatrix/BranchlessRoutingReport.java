package dev.vessel.kernel.testmatrix;

/**
 * Immutable report validating O(1) branchless jump table dispatch latency and throughput.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param registeredRoutes count of registered jump routes in the table
 * @param tableCapacity total capacity of the flat dispatch table
 * @param routedInvocations total number of operations dispatched
 * @param averageDispatchNanos mean dispatch latency in nanoseconds
 * @param totalDurationNanos total elapsed benchmark or test duration
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record BranchlessRoutingReport(
        int registeredRoutes,
        int tableCapacity,
        long routedInvocations,
        double averageDispatchNanos,
        long totalDurationNanos
) {
}
