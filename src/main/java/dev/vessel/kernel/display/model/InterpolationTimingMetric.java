package dev.vessel.kernel.display.model;

/**
 * Immutable telemetry metric recording dynamic TPS-aware interpolation duration
 * calculations for Minecraft Display Entities.
 *
 * @param sampleTimestampNanos timestamp when the TPS sample was evaluated
 * @param measuredTps current rolling tick rate
 * @param calculatedDurationTicks dynamically calculated interpolation duration in ticks
 * @param lerpSmoothingFactor fractional lerp factor applied to position delta
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record InterpolationTimingMetric(
        long sampleTimestampNanos,
        double measuredTps,
        int calculatedDurationTicks,
        float lerpSmoothingFactor
) {
    public InterpolationTimingMetric {
        if (measuredTps < 0.0) {
            throw new IllegalArgumentException("measuredTps must be non-negative: " + measuredTps);
        }
        if (calculatedDurationTicks < 1) {
            throw new IllegalArgumentException("calculatedDurationTicks must be at least 1: " + calculatedDurationTicks);
        }
    }
}
