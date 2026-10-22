package dev.vessel.kernel.actor;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Diagnostic metrics tracker for the Universal Harmonizer Request-Reply barrier.
 * <p>
 * Monitors cross-region latency, blocked time budgets, and timeout frequency to provide
 * profiling telemetry and identify legacy plugins causing thread contention.
 * </p>
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-5)
 */
public class CrossRegionHarmonizerMetrics {

    private final AtomicLong totalQueries = new AtomicLong(0L);
    private final AtomicLong totalBlockedNanos = new AtomicLong(0L);
    private final AtomicLong maxWaitNanos = new AtomicLong(0L);
    private final AtomicLong timeouts = new AtomicLong(0L);

    /**
     * Records a completed barrier query.
     *
     * @param elapsedNanos duration spent waiting for target strand reply
     */
    public void recordSuccess(long elapsedNanos) {
        totalQueries.incrementAndGet();
        totalBlockedNanos.addAndGet(elapsedNanos);

        long currentMax = maxWaitNanos.get();
        while (elapsedNanos > currentMax) {
            if (maxWaitNanos.compareAndSet(currentMax, elapsedNanos)) {
                break;
            }
            currentMax = maxWaitNanos.get();
        }
    }

    /**
     * Records a query that reached the barrier timeout threshold.
     */
    public void recordTimeout() {
        totalQueries.incrementAndGet();
        timeouts.incrementAndGet();
    }

    public long getTotalQueries() {
        return totalQueries.get();
    }

    public long getTotalBlockedNanos() {
        return totalBlockedNanos.get();
    }

    public long getMaxWaitNanos() {
        return maxWaitNanos.get();
    }

    public long getTimeouts() {
        return timeouts.get();
    }

    public double getAverageWaitMillis() {
        long queries = totalQueries.get();
        if (queries == 0) {
            return 0.0;
        }
        return (totalBlockedNanos.get() / (double) queries) / 1_000_000.0;
    }

    public void reset() {
        totalQueries.set(0L);
        totalBlockedNanos.set(0L);
        maxWaitNanos.set(0L);
        timeouts.set(0L);
    }
}
