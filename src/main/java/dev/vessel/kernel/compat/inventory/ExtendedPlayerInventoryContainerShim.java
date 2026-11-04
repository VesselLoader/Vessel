package dev.vessel.kernel.compat.inventory;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Shims extended player inventory and accessory slots (Curios API, Accessories)
 * without corrupting vanilla window packet sync.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public final class ExtendedPlayerInventoryContainerShim {

    private final ConcurrentMap<UUID, ConcurrentMap<String, Object>> playerAccessorySlots = new ConcurrentHashMap<>();

    public void setEquippedItem(UUID playerUuid, String slotIdentifier, Object itemStack) {
        Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        Objects.requireNonNull(slotIdentifier, "slotIdentifier cannot be null");
        playerAccessorySlots.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>()).put(slotIdentifier, itemStack);
    }

    public Object getEquippedItem(UUID playerUuid, String slotIdentifier) {
        if (playerUuid == null || slotIdentifier == null) {
            return null;
        }
        ConcurrentMap<String, Object> slots = playerAccessorySlots.get(playerUuid);
        return slots != null ? slots.get(slotIdentifier) : null;
    }

    public boolean hasEquippedItem(UUID playerUuid, String slotIdentifier) {
        if (playerUuid == null || slotIdentifier == null) {
            return false;
        }
        ConcurrentMap<String, Object> slots = playerAccessorySlots.get(playerUuid);
        return slots != null && slots.containsKey(slotIdentifier);
    }

    public void clearPlayerInventory(UUID playerUuid) {
        if (playerUuid != null) {
            playerAccessorySlots.remove(playerUuid);
        }
    }
}
