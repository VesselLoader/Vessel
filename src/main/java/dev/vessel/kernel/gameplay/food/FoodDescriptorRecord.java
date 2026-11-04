package dev.vessel.kernel.gameplay.food;

/**
 * Immutable descriptor representing food nutrition, saturation, duration, and edible flags.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public record FoodDescriptorRecord(
        int nutrition,
        float saturation,
        int eatDurationTicks,
        boolean alwaysEdible
) {
}
