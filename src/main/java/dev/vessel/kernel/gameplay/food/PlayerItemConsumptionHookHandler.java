package dev.vessel.kernel.gameplay.food;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Handles server-side item consumption completion hooks for players eating modded food items.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public final class PlayerItemConsumptionHookHandler {

    private final ConcurrentMap<UUID, Integer> playerTotalConsumedItems = new ConcurrentHashMap<>();

    public boolean onFoodConsumed(UUID playerUuid, String itemIdentifier, int nutritionGained, float saturationGained) {
        Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        Objects.requireNonNull(itemIdentifier, "itemIdentifier cannot be null");
        playerTotalConsumedItems.merge(playerUuid, 1, Integer::sum);
        return true;
    }

    public int getPlayerConsumedCount(UUID playerUuid) {
        if (playerUuid == null) {
            return 0;
        }
        return playerTotalConsumedItems.getOrDefault(playerUuid, 0);
    }

    public void resetPlayerCount(UUID playerUuid) {
        if (playerUuid != null) {
            playerTotalConsumedItems.remove(playerUuid);
        }
    }
}
