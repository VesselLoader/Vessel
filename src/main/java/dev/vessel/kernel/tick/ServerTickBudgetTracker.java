package dev.vessel.kernel.tick;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Allocation-free rolling MSPT (Milliseconds Per Tick) and TPS monitor.
 * <p>
 * Employs a fixed circular ring buffer of 100 tick measurements to compute rolling averages,
 * peak spikes, and dynamic TPS estimates with zero heap allocations during gameplay.
 * </p>
 *
 * @version 1.0.1
 * @since 1.0.0 (Pass K-6)
 */
public class ServerTickBudgetTracker {

    private static final int RING_SIZE = 100;
    private static final double TARGET_TPS = 20.0;
    private static final double TARGET_MSPT = 50.0;

    private final long[] ring = new long[RING_SIZE];
    private final AtomicInteger writeIndex = new AtomicInteger(0);
    private final AtomicInteger count = new AtomicInteger(0);

    /**
     * Records elapsed duration of a finished game tick in nanoseconds.
     *
     * @param durationNanos execution time of the tick in nanoseconds
     */
    public synchronized void recordTick(long durationNanos) {
        int idx = writeIndex.getAndIncrement() % RING_SIZE;
        ring[idx] = Math.max(0L, durationNanos);
        if (count.get() < RING_SIZE) {
            count.incrementAndGet();
        }
    }

    /**
     * Returns average MSPT across the recorded rolling window.
     *
     * @return average milliseconds per tick
     */
    public synchronized double getAverageMspt() {
        int samples = count.get();
        if (samples == 0) {
            return 0.0;
        }
        long sum = 0L;
        for (int i = 0; i < samples; i++) {
            sum += ring[i];
        }
        return (sum / (double) samples) / 1_000_000.0;
    }

    /**
     * Returns maximum peak MSPT in the rolling window.
     *
     * @return peak MSPT in milliseconds
     */
    public synchronized double getMaxMspt() {
        int samples = count.get();
        if (samples == 0) {
            return 0.0;
        }
        long max = 0L;
        for (int i = 0; i < samples; i++) {
            if (ring[i] > max) {
                max = ring[i];
            }
        }
        return max / 1_000_000.0;
    }

    /**
     * Returns minimum MSPT in the rolling window.
     *
     * @return minimum MSPT in milliseconds
     */
    public synchronized double getMinMspt() {
        int samples = count.get();
        if (samples == 0) {
            return 0.0;
        }
        long min = Long.MAX_VALUE;
        for (int i = 0; i < samples; i++) {
            if (ring[i] < min) {
                min = ring[i];
            }
        }
        return min / 1_000_000.0;
    }

    /**
     * Computes real-time TPS based on rolling MSPT.
     *
     * @return current TPS (capped at 20.0)
     */
    public synchronized double getCalculatedTps() {
        double avgMspt = getAverageMspt();
        if (avgMspt <= 0.0) {
            return TARGET_TPS;
        }
        if (avgMspt <= TARGET_MSPT) {
            return TARGET_TPS;
        }
        return Math.min(TARGET_TPS, 1000.0 / avgMspt);
    }

    public synchronized int getSampleCount() {
        return count.get();
    }

    public synchronized void reset() {
        writeIndex.set(0);
        count.set(0);
    }
}
