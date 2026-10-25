package dev.vessel.kernel.adapter.forge;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable report detailing registry key collisions encountered across candidate mod entries.
 *
 * @param hasCollisions  flag indicating whether at least one key collision occurred
 * @param collisionCount total collision count
 * @param collisions     list of specific pairwise collisions
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record RegistryCollisionReport(
        boolean hasCollisions,
        int collisionCount,
        List<RegistryKeyCollision> collisions
) {

    /**
     * Compact constructor validating non-null list.
     */
    public RegistryCollisionReport {
        Objects.requireNonNull(collisions, "collisions must not be null");
        collisions = Collections.unmodifiableList(collisions);
    }
}
