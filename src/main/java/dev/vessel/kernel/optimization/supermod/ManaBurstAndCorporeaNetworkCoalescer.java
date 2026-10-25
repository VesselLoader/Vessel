package dev.vessel.kernel.optimization.supermod;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Optimizes Botania, MythicBotany, and Nature's Aura networks by converting unobstructed static
 * {@code EntityManaBurst} trajectories into zero-entity mathematical transfers and indexing
 * Corporea Spark inventories in an {@code O(1)} hash table.
 *
 * @version 1.0.0
 */
public final class ManaBurstAndCorporeaNetworkCoalescer {

    private final Map<String, Long> scheduledVirtualTransfers = new ConcurrentHashMap<>();
    private final Map<String, Integer> scheduledManaAmounts = new ConcurrentHashMap<>();
    private final Map<String, Map<Long, Integer>> corporeaContainerCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> corporeaNetworkTotals = new ConcurrentHashMap<>();

    /**
     * Checks whether a Mana Spreader to Mana Pool burst can bypass entity spawning and execute as
     * a deterministic virtual mana transfer.
     *
     * @param spreaderPos      the packed 64-bit Mana Spreader block position
     * @param targetPoolPos    the packed 64-bit target Mana Pool block position
     * @param hasDynamicLens   whether the spreader uses a physics-altering dynamic lens
     * @param lineOfSightClear whether the raycast line of sight between spreader and pool is clear
     * @return {@code true} if the burst can be virtualized with zero entity spawns
     */
    public boolean canConvertManaBurstToVirtualTransfer(
            long spreaderPos,
            long targetPoolPos,
            boolean hasDynamicLens,
            boolean lineOfSightClear) {
        return spreaderPos != targetPoolPos && !hasDynamicLens && lineOfSightClear;
    }

    /**
     * Schedules a virtual mana transfer to arrive after {@code travelTicks} with zero entity ticks.
     *
     * @param spreaderPos   the packed 64-bit Mana Spreader block position
     * @param targetPoolPos the packed 64-bit target Mana Pool block position
     * @param manaAmount    the mana payload to deliver
     * @param travelTicks   the flight duration in ticks
     * @param currentTick   the current monotonic server tick
     */
    public void scheduleVirtualManaTransfer(
            long spreaderPos,
            long targetPoolPos,
            int manaAmount,
            int travelTicks,
            long currentTick) {
        if (manaAmount <= 0) {
            return;
        }
        String linkKey = spreaderPos + "->" + targetPoolPos;
        long arrivalTick = currentTick + Math.max(1, travelTicks);
        scheduledVirtualTransfers.put(linkKey, arrivalTick);
        scheduledManaAmounts.merge(linkKey, manaAmount, Integer::sum);
    }

    /**
     * Indexes the item count of a container attached to a Botania Corporea Spark network for
     * {@code O(1)} lookup.
     *
     * @param sparkNetworkId the Corporea network identifier
     * @param containerPos   the packed 64-bit container block coordinate
     * @param itemId         the canonical item identifier
     * @param count          the updated item count in that container
     */
    public void indexCorporeaNetworkInventory(
            long sparkNetworkId,
            long containerPos,
            String itemId,
            int count) {
        Objects.requireNonNull(itemId, "itemId must not be null");
        String netItemKey = sparkNetworkId + "|" + itemId;
        int sanitizedCount = Math.max(0, count);
        Map<Long, Integer> perContainer = corporeaContainerCounts.computeIfAbsent(
                netItemKey, ignored -> new ConcurrentHashMap<>());
        Integer previous = perContainer.put(containerPos, sanitizedCount);
        int delta = sanitizedCount - (previous != null ? previous : 0);
        corporeaNetworkTotals
                .computeIfAbsent(netItemKey, ignored -> new AtomicInteger(0))
                .addAndGet(delta);
    }

    /**
     * Queries the total indexed count of an item across an entire Corporea Spark network in
     * {@code O(1)} time.
     *
     * @param sparkNetworkId the Corporea network identifier
     * @param itemId         the canonical item identifier
     * @return the total indexed item count across all containers on the spark network
     */
    public int queryCorporeaIndexedCount(long sparkNetworkId, String itemId) {
        Objects.requireNonNull(itemId, "itemId must not be null");
        AtomicInteger total = corporeaNetworkTotals.get(sparkNetworkId + "|" + itemId);
        return total != null ? Math.max(0, total.get()) : 0;
    }
}
