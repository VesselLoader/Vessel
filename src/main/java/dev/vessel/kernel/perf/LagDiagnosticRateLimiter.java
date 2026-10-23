package dev.vessel.kernel.perf;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe rate limiter designed to suppress diagnostic log spam and prevent disk I/O bottlenecks.
 * <p>
 * Ensures that high-frequency warnings (e.g., cross-region synchronization bottlenecks or malformed
 * metadata warnings) are emitted at most once within a configured time window per diagnostic key.
 * </p>
 *
 * @version 1.0.1
 * @since 1.0.0 (Pass K-5)
 */
public class LagDiagnosticRateLimiter {

    private final long intervalNanos;
    private final ConcurrentHashMap<String, AtomicLong> lastEmittedMap = new ConcurrentHashMap<>();

    /**
     * Constructs a rate limiter with the specified minimum interval in milliseconds.
     *
     * @param intervalMillis minimum delay between successive permitted events
     */
    public LagDiagnosticRateLimiter(long intervalMillis) {
        if (intervalMillis <= 0) {
            throw new IllegalArgumentException("intervalMillis must be positive: " + intervalMillis);
        }
        this.intervalNanos = intervalMillis * 1_000_000L;
    }

    /**
     * Evaluates whether an event with the given key is permitted to log/execute.
     *
     * @param key diagnostic category or identifier key
     * @return true if permitted, false if throttled
     */
    public boolean tryAcquire(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        long now = System.nanoTime();
        AtomicLong last = lastEmittedMap.computeIfAbsent(key, k -> new AtomicLong(0L));

        while (true) {
            long prev = last.get();
            if (now - prev < intervalNanos) {
                return false;
            }
            if (last.compareAndSet(prev, now)) {
                return true;
            }
        }
    }

    /**
     * Clears all rate limiter tracking history.
     */
    public void reset() {
        lastEmittedMap.clear();
    }
}
