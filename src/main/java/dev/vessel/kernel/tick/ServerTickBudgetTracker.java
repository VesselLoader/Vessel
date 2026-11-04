package dev.vessel.kernel.tick;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * Allocation-free, lock-free rolling MSPT (Milliseconds Per Tick) and TPS monitor.
 * <p>
 * Employs a power-of-two circular ring buffer of 128 tick measurements using bitwise index masking
 * and {@link AtomicLongArray} to compute rolling averages, peak spikes, and dynamic TPS estimates
 * with zero lock contention across concurrent regional Folia worker threads.
 * </p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @version 1.0.2
 * @since 1.0.0 (Pass K-6)
 */
public final class ServerTickBudgetTracker {

    private static final int RING_SIZE = 128;
    private static final int RING_MASK = RING_SIZE - 1; // 127
    private static final double TARGET_TPS = 20.0;
    private static final double TARGET_MSPT = 50.0;

    private final AtomicLongArray ring = new AtomicLongArray(RING_SIZE);
    private final AtomicInteger writeIndex = new AtomicInteger(0);
    private final AtomicInteger count = new AtomicInteger(0);

    /**
     * Constructs a lock-free rolling server tick budget tracker.
     */
    public ServerTickBudgetTracker() {
    }

    /**
     * Records elapsed duration of a finished game tick in nanoseconds without blocking or monitor locking.
     *
     * @param durationNanos execution time of the tick in nanoseconds
     */
    public void recordTick(long durationNanos) {
        int idx = (writeIndex.getAndIncrement() & RING_MASK);
        ring.set(idx, Math.max(0L, durationNanos));
        count.getAndUpdate(c -> (c < RING_SIZE) ? c + 1 : RING_SIZE);
    }

    /**
     * Returns average MSPT across the recorded rolling window.
     *
     * @return average milliseconds per tick
     */
    public double getAverageMspt() {
        int samples = count.get();
        if (samples == 0) {
            return 0.0;
        }
        long sum = 0L;
        for (int i = 0; i < samples; i++) {
            sum += ring.get(i);
        }
        return (sum / (double) samples) / 1_000_000.0;
    }

    /**
     * Returns maximum peak MSPT in the rolling window.
     *
     * @return peak MSPT in milliseconds
     */
    public double getMaxMspt() {
        int samples = count.get();
        if (samples == 0) {
            return 0.0;
        }
        long max = 0L;
        for (int i = 0; i < samples; i++) {
            long val = ring.get(i);
            if (val > max) {
                max = val;
            }
        }
        return max / 1_000_000.0;
    }

    /**
     * Returns minimum MSPT in the rolling window.
     *
     * @return minimum MSPT in milliseconds
     */
    public double getMinMspt() {
        int samples = count.get();
        if (samples == 0) {
            return 0.0;
        }
        long min = Long.MAX_VALUE;
        for (int i = 0; i < samples; i++) {
            long val = ring.get(i);
            if (val < min) {
                min = val;
            }
        }
        return min / 1_000_000.0;
    }

    /**
     * Computes real-time TPS based on rolling MSPT.
     *
     * @return current TPS (capped at 20.0)
     */
    public double getCalculatedTps() {
        double avgMspt = getAverageMspt();
        if (avgMspt <= 0.0 || avgMspt <= TARGET_MSPT) {
            return TARGET_TPS;
        }
        return Math.min(TARGET_TPS, 1000.0 / avgMspt);
    }

    /**
     * Returns the number of recorded tick samples currently stored in the rolling buffer.
     *
     * @return active sample count (0..128)
     */
    public int getSampleCount() {
        return count.get();
    }

    /**
     * Resets the rolling buffer and index counters.
     */
    public void reset() {
        writeIndex.set(0);
        count.set(0);
        for (int i = 0; i < RING_SIZE; i++) {
            ring.set(i, 0L);
        }
    }
}
