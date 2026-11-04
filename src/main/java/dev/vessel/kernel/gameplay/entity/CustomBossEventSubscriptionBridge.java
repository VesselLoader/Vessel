package dev.vessel.kernel.gameplay.entity;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Bridges server-side custom boss events (BossBar progress, color, overlay, visible players) with clients.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public final class CustomBossEventSubscriptionBridge {

    private final ConcurrentMap<UUID, Float> bossHealthPercentages = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Set<UUID>> subscribedPlayers = new ConcurrentHashMap<>();

    public void registerBoss(UUID bossUuid, float initialHealthPercent) {
        Objects.requireNonNull(bossUuid, "bossUuid cannot be null");
        bossHealthPercentages.put(bossUuid, Math.clamp(initialHealthPercent, 0.0f, 1.0f));
        subscribedPlayers.put(bossUuid, ConcurrentHashMap.newKeySet());
    }

    public void updateBossProgress(UUID bossUuid, float progressPercent) {
        if (bossUuid != null && bossHealthPercentages.containsKey(bossUuid)) {
            bossHealthPercentages.put(bossUuid, Math.clamp(progressPercent, 0.0f, 1.0f));
        }
    }

    public void subscribePlayer(UUID bossUuid, UUID playerUuid) {
        Objects.requireNonNull(bossUuid, "bossUuid cannot be null");
        Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        Set<UUID> set = subscribedPlayers.get(bossUuid);
        if (set != null) {
            set.add(playerUuid);
        }
    }

    public void unsubscribePlayer(UUID bossUuid, UUID playerUuid) {
        if (bossUuid != null && playerUuid != null) {
            Set<UUID> set = subscribedPlayers.get(bossUuid);
            if (set != null) {
                set.remove(playerUuid);
            }
        }
    }

    public float getBossProgress(UUID bossUuid) {
        if (bossUuid == null) {
            return 0.0f;
        }
        return bossHealthPercentages.getOrDefault(bossUuid, 0.0f);
    }

    public void removeBoss(UUID bossUuid) {
        if (bossUuid != null) {
            bossHealthPercentages.remove(bossUuid);
            subscribedPlayers.remove(bossUuid);
        }
    }
}
