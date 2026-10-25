package dev.vessel.kernel.sanitizer;

/**
 * Immutable report generated after executing a Metaspace ClassLoader leak sanitization sweep.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param trackedLoadersCount total number of monitored classloaders evaluated
 * @param leakedLoadersDetected number of classloaders retaining active strong references past reload
 * @param clearedThreadLocalsCount count of leaked ThreadLocal variables unhooked
 * @param clearedStaticCachesCount count of internal JVM static caches purged
 * @param reclaimedMetaspaceEstimatedBytes estimated Metaspace memory reclaimed in bytes
 * @param durationNanos execution time of the sanitization operation in nanoseconds
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record MetaspaceSanitizeReport(
        int trackedLoadersCount,
        int leakedLoadersDetected,
        int clearedThreadLocalsCount,
        int clearedStaticCachesCount,
        long reclaimedMetaspaceEstimatedBytes,
        long durationNanos
) {
    public boolean hasLeaksRemediated() {
        return clearedThreadLocalsCount > 0 || clearedStaticCachesCount > 0 || leakedLoadersDetected > 0;
    }
}
