package dev.vessel.kernel.tick;

import java.util.Objects;

/**
 * Configuration parameters defining horizontal and vertical 3D activation radii for an entity category.
 * <p>
 * Directly resolves ERR-41 and ERR-42.
 * </p>
 *
 * @param category entity category
 * @param horizontalRadius maximum horizontal distance in blocks
 * @param verticalRadius maximum vertical distance in blocks (clamping high-altitude and deep-cave ticking)
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record EarCategoryConfig(
        EarEntityTypeCategory category,
        double horizontalRadius,
        double verticalRadius
) {

    /**
     * Compact constructor validating invariants.
     */
    public EarCategoryConfig {
        Objects.requireNonNull(category, "category cannot be null");
        if (horizontalRadius <= 0.0) {
            throw new IllegalArgumentException("horizontalRadius must be positive: " + horizontalRadius);
        }
        if (verticalRadius <= 0.0) {
            throw new IllegalArgumentException("verticalRadius must be positive: " + verticalRadius);
        }
    }

    /**
     * Pre-computes horizontal radius squared for zero-allocation checks.
     *
     * @return horizontal radius squared
     */
    public double horizontalRadiusSquared() {
        return horizontalRadius * horizontalRadius;
    }

    /**
     * Pre-computes vertical radius squared for zero-allocation checks.
     *
     * @return vertical radius squared
     */
    public double verticalRadiusSquared() {
        return verticalRadius * verticalRadius;
    }
}
