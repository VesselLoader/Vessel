package dev.vessel.adapter.bridge;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Synchronizes extra accessory inventory slots from Curios API, Trinkets, and
 * Sophisticated Backpacks with Bukkit death/respawn lifecycle events so {@code keepInventory}
 * and grave plugins do not wipe or duplicate modded accessories.
 *
 * @version 1.0.0
 */
public final class CuriosAccessoriesInventoryBridge {

    private final Map<UUID, Map<String, String>> playerAccessorySlots = new ConcurrentHashMap<>();

    /**
     * Creates a new Curios/Trinkets accessories inventory bridge.
     */
    public CuriosAccessoriesInventoryBridge() {
    }

    /**
     * Sets or updates a serialized accessory item in a player's Curios/Trinkets slot.
     *
     * @param playerUuid         player UUID
     * @param slotIdentifier     slot identifier (e.g., {@code "curios:back:0"}, {@code "trinkets:chest/necklace"})
     * @param serializedItemData serialized item stack + NBT/components
     */
    public void setAccessorySlotItem(
            UUID playerUuid, String slotIdentifier, String serializedItemData) {
        Objects.requireNonNull(playerUuid, "playerUuid must not be null");
        Objects.requireNonNull(slotIdentifier, "slotIdentifier must not be null");
        Objects.requireNonNull(serializedItemData, "serializedItemData must not be null");
        Map<String, String> slots =
                playerAccessorySlots.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>());
        slots.put(slotIdentifier.trim(), serializedItemData);
    }

    /**
     * Captures a snapshot of the player's equipped accessories upon death and clears the
     * live slots if {@code keepInventoryRule} is {@code false} (preventing duplication when
     * grave plugins store the drops).
     *
     * @param playerUuid        player UUID
     * @param keepInventoryRule whether {@code keepInventory} or a Bukkit keep-inventory flag is active
     * @return unmodifiable snapshot of captured slot-to-item mappings
     */
    public Map<String, String> captureAccessoriesOnDeath(
            UUID playerUuid, boolean keepInventoryRule) {
        Objects.requireNonNull(playerUuid, "playerUuid must not be null");
        Map<String, String> slots = playerAccessorySlots.get(playerUuid);
        if (slots == null || slots.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> snapshot = new LinkedHashMap<>(slots);
        if (!keepInventoryRule) {
            slots.clear();
        }
        return Collections.unmodifiableMap(snapshot);
    }

    /**
     * Restores preserved Curios/Trinkets accessories onto the player upon respawn.
     *
     * @param playerUuid      player UUID
     * @param keptAccessories map of accessory slot identifiers to serialized item data
     */
    public void restoreAccessoriesOnRespawn(
            UUID playerUuid, Map<String, String> keptAccessories) {
        Objects.requireNonNull(playerUuid, "playerUuid must not be null");
        Objects.requireNonNull(keptAccessories, "keptAccessories must not be null");
        Map<String, String> slots =
                playerAccessorySlots.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>());
        slots.clear();
        slots.putAll(keptAccessories);
    }
}
