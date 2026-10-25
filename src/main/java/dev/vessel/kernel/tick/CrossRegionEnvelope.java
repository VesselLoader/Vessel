package dev.vessel.kernel.tick;

import java.util.Objects;

/**
 * Message envelope encapsulating an asynchronous cross-region actor dispatch.
 * <p>
 * Contains sender/receiver region keys, execution deadline, and executable workload.
 * </p>
 * <p>
 * Directly resolves ERR-31, ERR-32, and FOL-041..060.
 * </p>
 *
 * @param requestId unique identifier for request-reply tracking
 * @param sourceRegionKey packed region key of the dispatching actor
 * @param targetRegionKey packed region key of the receiving actor
 * @param enqueueTimestampNanos nanosecond timestamp when the envelope was created
 * @param timeoutMillis maximum allowable latency before timing out
 * @param task workload to execute on the destination actor strand
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record CrossRegionEnvelope(
        long requestId,
        long sourceRegionKey,
        long targetRegionKey,
        long enqueueTimestampNanos,
        long timeoutMillis,
        Runnable task
) {

    /**
     * Compact constructor validating invariants.
     */
    public CrossRegionEnvelope {
        Objects.requireNonNull(task, "task cannot be null");
        if (timeoutMillis < 0) {
            throw new IllegalArgumentException("timeoutMillis cannot be negative: " + timeoutMillis);
        }
    }

    /**
     * Evaluates if this envelope has exceeded its timeout deadline.
     *
     * @param currentNanos current monotonic time in nanoseconds
     * @return {@code true} if deadline has passed
     */
    public boolean isExpired(long currentNanos) {
        if (timeoutMillis == 0) {
            return false;
        }
        long elapsedMillis = (currentNanos - enqueueTimestampNanos) / 1_000_000L;
        return elapsedMillis > timeoutMillis;
    }
}
