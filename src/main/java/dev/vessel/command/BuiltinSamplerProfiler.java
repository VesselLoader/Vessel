package dev.vessel.command;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Built-in Spark-style low-overhead method sampler profiler (`BuiltinSamplerProfiler`) that
 * aggregates per-module and per-method execution nanoseconds into a flamegraph summary map.
 *
 * @version 1.0.1
 */
public final class BuiltinSamplerProfiler {

    private static final BuiltinSamplerProfiler DEFAULT_INSTANCE = new BuiltinSamplerProfiler();

    private final AtomicBoolean samplingActive = new AtomicBoolean(false);
    private final Map<String, AtomicLong> accumulatedSamplesNanos = new ConcurrentHashMap<>();

    /**
     * Returns the global default profiler instance.
     *
     * @return default profiler instance
     */
    public static BuiltinSamplerProfiler getDefaultProfiler() {
        return DEFAULT_INSTANCE;
    }

    /**
     * Checks whether profiling sampling is currently active.
     *
     * @return true if sampling is active
     */
    public boolean isSamplingActive() {
        return samplingActive.get();
    }

    /**
     * Returns the total count of distinct sampled method frames recorded.
     *
     * @return count of sampled method signatures
     */
    public int getSampleCount() {
        return accumulatedSamplesNanos.size();
    }

    /**
     * Clears previous samples and starts a new profiler sampling session.
     */
    public void startSamplingSession() {
        accumulatedSamplesNanos.clear();
        samplingActive.set(true);
    }

    /**
     * Records a method execution sample in nanoseconds if a sampling session is currently active.
     *
     * @param moduleAndMethodSignature the qualified module and method frame signature
     * @param durationNanos            the sampled execution duration in nanoseconds
     */
    public void recordMethodSample(String moduleAndMethodSignature, long durationNanos) {
        Objects.requireNonNull(
                moduleAndMethodSignature, "moduleAndMethodSignature must not be null");
        if (!samplingActive.get() || durationNanos <= 0L) {
            return;
        }
        accumulatedSamplesNanos
                .computeIfAbsent(moduleAndMethodSignature, ignored -> new AtomicLong(0L))
                .addAndGet(durationNanos);
    }

    /**
     * Stops the current sampling session and exports an immutable flamegraph summary map sorted by
     * descending total duration in nanoseconds.
     *
     * @return an immutable map from method signature to cumulative nanoseconds
     */
    public Map<String, Long> stopAndExportFlamegraphSummary() {
        samplingActive.set(false);
        Map<String, Long> sorted = new LinkedHashMap<>();
        accumulatedSamplesNanos.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().get(), a.getValue().get()))
                .forEach(entry -> sorted.put(entry.getKey(), entry.getValue().get()));
        return Map.copyOf(sorted);
    }
}
