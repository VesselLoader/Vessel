package dev.vessel.kernel.tick;

import java.util.Objects;

/**
 * Immutable evaluation result for a 3D Entity Activation Range query.
 * <p>
 * Directly resolves ERR-41 and ERR-42.
 * </p>
 *
 * @param shouldTick {@code true} if the entity is inside activation range or immune
 * @param isImmune {@code true} if activation was granted due to immunity state
 * @param distanceSquared Euclidean distance squared between entity and player
 * @param category entity classification category
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record Ear3dEvaluationResult(
        boolean shouldTick,
        boolean isImmune,
        double distanceSquared,
        EarEntityTypeCategory category
) {

    /**
     * Compact constructor validating invariants.
     */
    public Ear3dEvaluationResult {
        Objects.requireNonNull(category, "category cannot be null");
        if (distanceSquared < 0.0) {
            throw new IllegalArgumentException("distanceSquared cannot be negative: " + distanceSquared);
        }
    }
}
