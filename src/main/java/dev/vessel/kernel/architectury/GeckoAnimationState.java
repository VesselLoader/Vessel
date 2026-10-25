package dev.vessel.kernel.architectury;

import java.util.Objects;

/**
 * Immutable record capturing the server-side runtime state of an active GeckoLib animation controller.
 *
 * @param animatableId the unique entity or block entity identifier
 * @param controllerName the name of the animation controller
 * @param animationName the name of the active animation clip
 * @param startTick the world tick when this animation started
 * @param durationTicks expected duration in ticks (-1 if indefinite or looping)
 * @param loop whether the animation loops continuously
 * @param transitionSpeed playback transition speed multiplier
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record GeckoAnimationState(
        String animatableId,
        String controllerName,
        String animationName,
        long startTick,
        long durationTicks,
        boolean loop,
        double transitionSpeed
) {

    /**
     * Compact constructor validating non-null arguments.
     */
    public GeckoAnimationState {
        Objects.requireNonNull(animatableId, "animatableId cannot be null");
        Objects.requireNonNull(controllerName, "controllerName cannot be null");
        Objects.requireNonNull(animationName, "animationName cannot be null");
    }

    /**
     * Checks if this animation has concluded at the given tick.
     *
     * @param currentTick the current server world tick
     * @return true if concluded, false if still active or looping
     */
    public boolean hasConcluded(long currentTick) {
        if (loop || durationTicks < 0) {
            return false;
        }
        return currentTick >= (startTick + durationTicks);
    }
}
