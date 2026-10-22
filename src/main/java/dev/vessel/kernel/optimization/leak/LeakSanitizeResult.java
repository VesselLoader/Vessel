package dev.vessel.kernel.optimization.leak;

/**
 * Immutable report generated after performing a ThreadLocal and ClassLoader leak sanitization sweep.
 *
 * @param scannedThreadsCount number of active threads inspected
 * @param clearedThreadLocalsCount count of leaked ThreadLocal values or references unhooked
 * @param clearedStaticReferencesCount count of static cache structures sanitized
 * @param elapsedNanos duration of the sanitization operation in nanoseconds
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record LeakSanitizeResult(
        int scannedThreadsCount,
        int clearedThreadLocalsCount,
        int clearedStaticReferencesCount,
        long elapsedNanos
) {
    /**
     * Checks if any leaks were identified and eliminated.
     *
     * @return true if leaks were remediated
     */
    public boolean hasLeaksRemediated() {
        return clearedThreadLocalsCount > 0 || clearedStaticReferencesCount > 0;
    }
}
