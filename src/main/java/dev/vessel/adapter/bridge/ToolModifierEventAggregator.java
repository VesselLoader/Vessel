package dev.vessel.adapter.bridge;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aggregates per-hit and per-block-break modifier deltas from Tinkers' Construct,
 * Silent Gear, and Tetra tools into a single consolidated event evaluation per tick,
 * eliminating redundant Bukkit {@code PlayerItemDamageEvent} / {@code EntityDamageByEntityEvent}
 * spam.
 *
 * @version 1.0.0
 */
public final class ToolModifierEventAggregator {

    private final Map<Long, Float> aggregatedDamageBonus = new ConcurrentHashMap<>();
    private final Map<Long, Integer> aggregatedDurabilityCost = new ConcurrentHashMap<>();

    /**
     * Creates a new tool modifier event aggregator.
     */
    public ToolModifierEventAggregator() {
    }

    /**
     * Queues a bonus damage delta and durability cost delta for a modded tool instance.
     *
     * @param toolInstanceId      64-bit tool instance identifier
     * @param bonusDamageDelta    incremental bonus damage from a tool trait/modifier
     * @param durabilityCostDelta incremental durability cost from a tool trait/modifier
     */
    public void queueModifierDelta(
            long toolInstanceId, float bonusDamageDelta, int durabilityCostDelta) {
        if (Float.isFinite(bonusDamageDelta)) {
            aggregatedDamageBonus.merge(toolInstanceId, bonusDamageDelta, Float::sum);
        }
        if (durabilityCostDelta > 0) {
            aggregatedDurabilityCost.merge(toolInstanceId, durabilityCostDelta, Integer::sum);
        }
    }

    /**
     * Flushes and returns the accumulated bonus damage for the given tool instance.
     *
     * @param toolInstanceId 64-bit tool instance identifier
     * @return total accumulated bonus damage (0.0f if none)
     */
    public float flushAggregatedDamageBonus(long toolInstanceId) {
        Float removed = aggregatedDamageBonus.remove(toolInstanceId);
        return removed != null ? removed : 0.0f;
    }

    /**
     * Flushes and returns the accumulated durability cost for the given tool instance.
     *
     * @param toolInstanceId 64-bit tool instance identifier
     * @return total accumulated durability cost (0 if none)
     */
    public int flushAggregatedDurabilityCost(long toolInstanceId) {
        Integer removed = aggregatedDurabilityCost.remove(toolInstanceId);
        return removed != null ? removed : 0;
    }
}
