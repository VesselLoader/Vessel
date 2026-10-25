package dev.vessel.kernel.config;

import java.util.Objects;

/**
 * Immutable report summarizing the single-cycle cross-loader configuration synchronization.
 *
 * @param keysSynchronized count of configuration keys successfully synchronized
 * @param commentsPreserved count of comment lines preserved across formats
 * @param durationNanos execution time in nanoseconds
 * @param successful whether synchronization completed without errors
 * @param errorMessage error description if synchronization failed, or empty string
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record ConfigSyncReport(
        int keysSynchronized,
        int commentsPreserved,
        long durationNanos,
        boolean successful,
        String errorMessage
) {
    public ConfigSyncReport {
        if (keysSynchronized < 0) {
            keysSynchronized = 0;
        }
        if (commentsPreserved < 0) {
            commentsPreserved = 0;
        }
        if (durationNanos < 0) {
            durationNanos = 0;
        }
        errorMessage = Objects.requireNonNullElse(errorMessage, "");
    }

    /**
     * Creates a successful synchronization report.
     *
     * @param keysSynced count of synchronized keys
     * @param commentsPreserved count of preserved comments
     * @param durationNanos elapsed nanoseconds
     * @return successful report instance
     */
    public static ConfigSyncReport success(int keysSynced, int commentsPreserved, long durationNanos) {
        return new ConfigSyncReport(keysSynced, commentsPreserved, durationNanos, true, "");
    }

    /**
     * Creates a failed synchronization report.
     *
     * @param errorMessage reason for failure
     * @param durationNanos elapsed nanoseconds
     * @return failure report instance
     */
    public static ConfigSyncReport failure(String errorMessage, long durationNanos) {
        return new ConfigSyncReport(0, 0, durationNanos, false, errorMessage);
    }
}
