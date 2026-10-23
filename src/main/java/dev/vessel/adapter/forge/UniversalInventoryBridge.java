package dev.vessel.adapter.forge;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridges modded container slots ({@code IItemHandler} / Fabric {@code Storage}) into
 * a unified slot view accessible to Bukkit/Paper inventory inspection plugins
 * (e.g., DeluxeMenus, InvSee++, OpenInv).
 *
 * @version 1.0.0
 */
public final class UniversalInventoryBridge {

    private static final String EMPTY_SLOT = "minecraft:air*0";

    private final Map<String, Integer> containerSlotCounts = new ConcurrentHashMap<>();
    private final Map<String, String> slotContents = new ConcurrentHashMap<>();

    /**
     * Creates a new universal inventory bridge.
     */
    public UniversalInventoryBridge() {
    }

    /**
     * Returns the number of bridged slots for the specified modded container ID.
     *
     * @param containerId unique container identifier
     * @return number of bridged slots (0 if unknown)
     */
    public int getBridgedContainerSlotCount(String containerId) {
        Objects.requireNonNull(containerId, "containerId must not be null");
        return containerSlotCounts.getOrDefault(containerId.trim(), 0);
    }

    /**
     * Returns the serialized item representation ({@code "<itemId>*<count>"}) stored in
     * the specified modded container slot.
     *
     * @param containerId unique container identifier
     * @param slotIndex   zero-based slot index
     * @return item descriptor in the slot, or {@code "minecraft:air*0"} if empty
     */
    public String getItemInModdedSlot(String containerId, int slotIndex) {
        Objects.requireNonNull(containerId, "containerId must not be null");
        if (slotIndex < 0) {
            return EMPTY_SLOT;
        }
        String compositeKey = containerId.trim() + "#" + slotIndex;
        return slotContents.getOrDefault(compositeKey, EMPTY_SLOT);
    }

    /**
     * Updates the item stored in a modded container slot and expands the tracked slot
     * count for that container if needed.
     *
     * @param containerId unique container identifier
     * @param slotIndex   zero-based slot index
     * @param itemId      namespaced item identifier
     * @param count       stack count
     * @return {@code true} if the slot mutation succeeded
     */
    public boolean setItemInModdedSlot(
            String containerId, int slotIndex, String itemId, int count) {
        Objects.requireNonNull(containerId, "containerId must not be null");
        Objects.requireNonNull(itemId, "itemId must not be null");
        if (slotIndex < 0 || count < 0) {
            return false;
        }
        String normalizedContainer = containerId.trim();
        containerSlotCounts.merge(
                normalizedContainer,
                slotIndex + 1,
                Math::max);
        String compositeKey = normalizedContainer + "#" + slotIndex;
        if (count == 0 || "minecraft:air".equalsIgnoreCase(itemId.trim())) {
            slotContents.put(compositeKey, EMPTY_SLOT);
        } else {
            slotContents.put(compositeKey, itemId.trim() + "*" + count);
        }
        return true;
    }
}
