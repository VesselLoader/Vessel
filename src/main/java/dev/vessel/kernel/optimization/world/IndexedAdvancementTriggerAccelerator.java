package dev.vessel.kernel.optimization.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Replaces full 36-slot $\times$ 1,500-advancement inventory scans (ATM9/10, RLCraft,
 * BlazeandCave) with an inverted {@code rawItemId -> List<AdvancementId>} index evaluated
 * solely on the single inventory slot that actually changed.
 *
 * @version 1.0.0
 */
public final class IndexedAdvancementTriggerAccelerator {

    private final Map<Integer, List<String>> advancementsByItemRawId = new ConcurrentHashMap<>();
    private final Map<String, Integer> lastSeenItemInPlayerSlot = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> grantedAdvancementsByPlayer = new ConcurrentHashMap<>();

    /**
     * Creates a new indexed advancement trigger accelerator.
     */
    public IndexedAdvancementTriggerAccelerator() {
    }

    /**
     * Registers an {@code inventory_changed} item criterion into the inverted lookup table.
     *
     * @param advancementId     namespaced advancement identifier
     * @param requiredItemRawId numeric raw item ID required by the criterion
     */
    public void registerItemCriterion(String advancementId, int requiredItemRawId) {
        Objects.requireNonNull(advancementId, "advancementId must not be null");
        List<String> list =
                advancementsByItemRawId.computeIfAbsent(
                        requiredItemRawId, k -> new CopyOnWriteArrayList<>());
        String trimmed = advancementId.trim();
        if (!list.contains(trimmed)) {
            list.add(trimmed);
        }
    }

    /**
     * Evaluates advancement criteria exclusively for the single mutated inventory slot
     * {@code changedSlotIndex} and returns newly triggered advancements for the player.
     *
     * @param playerUuid       UUID of the player whose slot mutated
     * @param changedSlotIndex mutated inventory slot index
     * @param newItemRawId     numeric raw item ID now occupying the slot (0 for air)
     * @return unmodifiable list of newly unlocked advancement IDs
     */
    public List<String> evaluateDirtyInventorySlotOnly(
            UUID playerUuid, int changedSlotIndex, int newItemRawId) {
        Objects.requireNonNull(playerUuid, "playerUuid must not be null");
        String slotKey = playerUuid + ":" + changedSlotIndex;
        Integer previousItem = lastSeenItemInPlayerSlot.put(slotKey, newItemRawId);
        if (previousItem != null && previousItem == newItemRawId) {
            return Collections.emptyList();
        }
        if (newItemRawId <= 0) {
            return Collections.emptyList();
        }
        List<String> candidates = advancementsByItemRawId.get(newItemRawId);
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> granted =
                grantedAdvancementsByPlayer.computeIfAbsent(
                        playerUuid, k -> ConcurrentHashMap.newKeySet());
        List<String> newlyTriggered = new ArrayList<>();
        for (String advancementId : candidates) {
            if (granted.add(advancementId)) {
                newlyTriggered.add(advancementId);
            }
        }
        return Collections.unmodifiableList(newlyTriggered);
    }
}
