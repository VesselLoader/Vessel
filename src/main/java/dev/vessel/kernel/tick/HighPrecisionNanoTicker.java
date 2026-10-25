package dev.vessel.kernel.tick;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * High-precision nano-second tick scheduler for maintaining rock-solid 20.0 TPS.
 * <p>
 * Employs a hybrid precision pacing strategy:
 * <ul>
 *   <li>Coarse sleep via {@link LockSupport#parkNanos(long)} when remaining tick budget &gt; 2ms,
 *       releasing CPU core to other worker threads.</li>
 *   <li>Sub-millisecond spin-wait pacing when remaining budget &le; 2ms, eliminating OS timer
 *       jitter and keeping clock drift below 0.05ms.</li>
 * </ul>
 * </p>
 *
 * @version 1.0.1
 * @since 1.0.0 (Pass K-6)
 */
public class HighPrecisionNanoTicker {

    /** Exactly 50,000,000 nanoseconds (50 milliseconds) per game tick. */
    public static final long TICK_DURATION_NANOS = 50_000_000L;

    /** Coarse sleep threshold: 2 milliseconds in nanoseconds. */
    private static final long COARSE_PARK_THRESHOLD_NANOS = 2_000_000L;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong tickCount = new AtomicLong(0L);
    private volatile long nextTickTargetNanos = 0L;

    /**
     * Initializes the ticker clock baseline.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            this.tickCount.set(0L);
            this.nextTickTargetNanos = System.nanoTime() + TICK_DURATION_NANOS;
        }
    }

    /**
     * Halts the ticker.
     */
    public void stop() {
        running.set(false);
    }

    public boolean isRunning() {
        return running.get();
    }

    public long getTickCount() {
        return tickCount.get();
    }

    public long getNextTickTargetNanos() {
        return nextTickTargetNanos;
    }

    /**
     * Suspends execution with hybrid nano-pacing until the scheduled start of the next tick.
     *
     * @return actual nanoseconds spent waiting (positive) or overdue overrun (negative)
     */
    public long awaitNextTick() {
        if (!running.get()) {
            return 0L;
        }

        long now = System.nanoTime();
        long diff = nextTickTargetNanos - now;

        while (diff > 0 && running.get()) {
            if (diff > COARSE_PARK_THRESHOLD_NANOS) {
                // Coarse park: sleep for majority of remaining duration minus 1ms guard
                LockSupport.parkNanos(diff - 1_000_000L);
            } else {
                // Fine-grained busy-spin loop for the final micro-seconds
                Thread.onSpinWait();
            }
            now = System.nanoTime();
            diff = nextTickTargetNanos - now;
        }

        // Advance to next tick schedule target
        tickCount.incrementAndGet();
        nextTickTargetNanos += TICK_DURATION_NANOS;

        // Catch-up guard: if server overloaded and fallen behind by more than 2 ticks, re-anchor baseline
        if (now - nextTickTargetNanos > (TICK_DURATION_NANOS * 2)) {
            nextTickTargetNanos = now + TICK_DURATION_NANOS;
        }

        return -diff;
    }
}
