package dev.vessel.kernel.datapack;

import java.util.Objects;

/**
 * Immutable flat binary decision node for precomputed loot table evaluation in RAM.
 *
 * @param conditionMask 64-bit bitmask encoding required context conditions
 * @param jumpOffsetIfTrue relative jump offset if condition is satisfied
 * @param jumpOffsetIfFalse relative jump offset if condition fails
 * @param isLeaf whether this node produces a drop item
 * @param dropItemId item identifier if leaf node, or empty string
 * @param minCount minimum item count
 * @param maxCount maximum item count
 * @param dropChance base drop probability [0.0, 1.0]
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record PrecomputedBinaryLootNode(
        long conditionMask,
        int jumpOffsetIfTrue,
        int jumpOffsetIfFalse,
        boolean isLeaf,
        String dropItemId,
        int minCount,
        int maxCount,
        float dropChance
) {
    public PrecomputedBinaryLootNode {
        dropItemId = Objects.requireNonNullElse(dropItemId, "");
        if (minCount < 0) minCount = 0;
        if (maxCount < minCount) maxCount = minCount;
        if (dropChance < 0.0f) dropChance = 0.0f;
        if (dropChance > 1.0f) dropChance = 1.0f;
    }

    /**
     * Checks if this node's conditions are satisfied by the current context bitmask.
     *
     * @param contextMask 64-bit contextual bitmask
     * @return true if all condition bits are present
     */
    public boolean test(long contextMask) {
        return (conditionMask & contextMask) == conditionMask;
    }
}
