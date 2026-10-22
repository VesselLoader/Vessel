package dev.vessel.kernel.datapack;

import java.util.Objects;

/**
 * Pre-compiled immutable loot table drop rule evaluated via 64-bit condition bitmask.
 *
 * @param ruleId unique identifier for this loot rule
 * @param tableId namespaced loot table identifier (e.g., "minecraft:entities/zombie")
 * @param conditionMask 64-bit bitmask encoding required context conditions (player kill, silk touch, fire, etc.)
 * @param dropItemId namespaced dropped item identifier
 * @param minCount minimum drop amount
 * @param maxCount maximum drop amount
 * @param dropChance base drop probability between 0.0 and 1.0
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record OptimizedLootTableRule(
        String ruleId,
        String tableId,
        long conditionMask,
        String dropItemId,
        int minCount,
        int maxCount,
        float dropChance
) {
    public OptimizedLootTableRule {
        Objects.requireNonNull(ruleId, "ruleId cannot be null");
        Objects.requireNonNull(tableId, "tableId cannot be null");
        Objects.requireNonNull(dropItemId, "dropItemId cannot be null");
        if (minCount < 0) minCount = 0;
        if (maxCount < minCount) maxCount = minCount;
        if (dropChance < 0.0f) dropChance = 0.0f;
        if (dropChance > 1.0f) dropChance = 1.0f;
    }

    /**
     * Checks if this rule's conditions are satisfied by the given contextual bitmask.
     *
     * @param contextMask 64-bit context bitmask
     * @return true if all required condition bits are present
     */
    public boolean satisfiesConditions(long contextMask) {
        return (conditionMask & contextMask) == conditionMask;
    }
}
