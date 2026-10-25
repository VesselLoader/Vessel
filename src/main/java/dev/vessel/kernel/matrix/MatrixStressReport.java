package dev.vessel.kernel.matrix;

/**
 * Immutable report summarizing execution outcomes across the compatibility matrix.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record MatrixStressReport(
        int totalEvaluations,
        int passCount,
        int warningCount,
        int failCriticalCount,
        long elapsedNanos
) {
    /**
     * Checks if the overall evaluation succeeded without critical errors.
     *
     * @return true if failCriticalCount is zero
     */
    public boolean isSuccessful() {
        return failCriticalCount == 0;
    }

    /**
     * Returns the pass rate percentage.
     *
     * @return percentage from 0.0 to 100.0
     */
    public double getPassRatePercent() {
        if (totalEvaluations == 0) return 100.0;
        return ((double) (passCount + warningCount) / totalEvaluations) * 100.0;
    }
}
