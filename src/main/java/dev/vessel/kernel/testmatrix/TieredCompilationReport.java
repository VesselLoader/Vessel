package dev.vessel.kernel.testmatrix;

/**
 * Immutable report generated after executing tiered JIT compilation priority warmup passes.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param totalTargetsCompiled count of targets warmed up
 * @param hotPathsPromoted count of C2 hot-path targets executed past Tier 4 compilation threshold
 * @param totalInvocations total method warmup invocations executed
 * @param durationNanos total duration in nanoseconds
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record TieredCompilationReport(
        int totalTargetsCompiled,
        int hotPathsPromoted,
        long totalInvocations,
        long durationNanos
) {
}
