package dev.vessel.kernel.display.model;

import java.util.Objects;

/**
 * Adaptive timing smoother for Minecraft Display Entity interpolation metadata (interpolation_duration).
 * Dynamically adjusts client-side entity lerp duration based on real-time server TPS fluctuations
 * and tick latency jitter, eliminating visual stutter and teleports (extinguishes AST-OPT).
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class DisplayEntityInterpolationSmoother {

    public static final double NOMINAL_TPS = 20.0;
    public static final int DEFAULT_BASE_INTERPOLATION_TICKS = 3;
    public static final int MIN_INTERPOLATION_TICKS = 1;
    public static final int MAX_INTERPOLATION_TICKS = 20;
    public static final double SMOOTHING_ALPHA = 0.25; // EMA alpha weight

    private final int baseInterpolationTicks;
    private final int maxInterpolationTicks;
    private double smoothedTps = NOMINAL_TPS;
    private int currentInterpolationDuration = DEFAULT_BASE_INTERPOLATION_TICKS;
    private float currentLerpFactor = 1.0f;

    /**
     * Constructs a smoother with default settings.
     */
    public DisplayEntityInterpolationSmoother() {
        this(DEFAULT_BASE_INTERPOLATION_TICKS, MAX_INTERPOLATION_TICKS);
    }

    /**
     * Constructs a smoother with custom bounds.
     *
     * @param baseInterpolationTicks nominal interpolation duration at 20 TPS
     * @param maxInterpolationTicks upper ceiling for duration under severe lag
     */
    public DisplayEntityInterpolationSmoother(int baseInterpolationTicks, int maxInterpolationTicks) {
        if (baseInterpolationTicks < MIN_INTERPOLATION_TICKS) {
            throw new IllegalArgumentException("baseInterpolationTicks must be >= " + MIN_INTERPOLATION_TICKS);
        }
        if (maxInterpolationTicks < baseInterpolationTicks) {
            throw new IllegalArgumentException("maxInterpolationTicks must be >= baseInterpolationTicks");
        }
        this.baseInterpolationTicks = baseInterpolationTicks;
        this.maxInterpolationTicks = maxInterpolationTicks;
        this.currentInterpolationDuration = baseInterpolationTicks;
    }

    /**
     * Updates measured server TPS and recalculates adaptive interpolation duration.
     *
     * @param instantTps instantaneous or 5-second rolling TPS sample [0.0, 20.0+]
     * @return telemetry snapshot of the updated metric
     */
    public synchronized InterpolationTimingMetric updateTps(double instantTps) {
        double clampedTps = Math.max(0.5, instantTps);

        // Exponential Moving Average filter to prevent abrupt jitter
        smoothedTps = (SMOOTHING_ALPHA * clampedTps) + ((1.0 - SMOOTHING_ALPHA) * smoothedTps);

        // Target duration inversely proportional to TPS
        double durationMultiplier = NOMINAL_TPS / smoothedTps;
        int targetTicks = (int) Math.round(baseInterpolationTicks * durationMultiplier);
        currentInterpolationDuration = Math.clamp(targetTicks, MIN_INTERPOLATION_TICKS, maxInterpolationTicks);

        // Normalized lerp factor
        currentLerpFactor = (float) Math.clamp(1.0 / (double) currentInterpolationDuration, 0.05, 1.0);

        return new InterpolationTimingMetric(
                System.nanoTime(),
                smoothedTps,
                currentInterpolationDuration,
                currentLerpFactor
        );
    }

    /**
     * Resolves the recommended interpolation duration for entity metadata synchronization.
     *
     * @return duration in ticks to write into Display Entity metadata
     */
    public synchronized int getInterpolationDurationTicks() {
        return currentInterpolationDuration;
    }

    /**
     * Current smoothed TPS calculation.
     */
    public synchronized double getSmoothedTps() {
        return smoothedTps;
    }

    /**
     * Current calculated position lerp factor.
     */
    public synchronized float getCurrentLerpFactor() {
        return currentLerpFactor;
    }

    public int getBaseInterpolationTicks() {
        return baseInterpolationTicks;
    }

    public int getMaxInterpolationTicks() {
        return maxInterpolationTicks;
    }
}
