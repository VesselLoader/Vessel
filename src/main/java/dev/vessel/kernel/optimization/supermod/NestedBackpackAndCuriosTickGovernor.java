package dev.vessel.kernel.optimization.supermod;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Governs Sophisticated Backpacks, Sophisticated Storage, Traveler's Backpack, and Curios API
 * inventory ticks. Coalesces all magnet upgrades per player into a single scan every 5 ticks,
 * blocks recursive nested backpack ticking, and skips upgrade evaluation when the inventory
 * version counter has not changed.
 *
 * @version 1.0.0
 */
public final class NestedBackpackAndCuriosTickGovernor {

    private static final int MAGNET_SCAN_INTERVAL_TICKS = 5;

    private final Map<UUID, Long> lastPlayerMagnetScanTick = new ConcurrentHashMap<>();
    private final Map<String, Long> evaluatedSlotVersions = new ConcurrentHashMap<>();

    /**
     * Coalesces all Magnet Upgrade entity queries across a player's backpacks and Curios slots
     * into a single scan every 5 ticks.
     *
     * @param playerUuid    the player's unique identifier
     * @param maxPullRadius the requested item pull radius in blocks
     * @param currentTick   the current monotonic server tick
     * @return {@code true} if the unified magnet scan should execute on this tick
     */
    public boolean coalescePlayerMagnetUpgradeScan(
            UUID playerUuid,
            double maxPullRadius,
            long currentTick) {
        Objects.requireNonNull(playerUuid, "playerUuid must not be null");
        if (maxPullRadius <= 0.0) {
            return false;
        }
        Long previousTick = lastPlayerMagnetScanTick.get(playerUuid);
        if (previousTick != null && (currentTick - previousTick) < MAGNET_SCAN_INTERVAL_TICKS) {
            return false;
        }
        lastPlayerMagnetScanTick.put(playerUuid, currentTick);
        return true;
    }

    /**
     * Suppresses recursive ticking when a backpack or storage bag is nested inside another
     * backpack ({@code containerDepth > 1}).
     *
     * @param containerDepth  the nesting depth ({@code 1} = top-level player slot)
     * @param containerItemId the canonical container item identifier
     * @return {@code true} if the nested container tick must be suppressed
     */
    public boolean suppressNestedContainerRecursiveTick(int containerDepth, String containerItemId) {
        Objects.requireNonNull(containerItemId, "containerItemId must not be null");
        return containerDepth > 1;
    }

    /**
     * Determines whether an inventory upgrade (Compacting, Feeding, Auto-Smelt) should run by
     * checking whether the inventory mutation version has advanced since the last evaluation.
     *
     * @param playerUuid              the player's unique identifier
     * @param slotIndex               the backpack or Curios slot index
     * @param currentInventoryVersion the current inventory mutation counter
     * @param lastEvaluatedVersion    the caller-tracked last evaluated version
     * @return {@code true} if the inventory version changed and the upgrade should evaluate
     */
    public boolean shouldEvaluateInventoryUpgrade(
            UUID playerUuid,
            int slotIndex,
            long currentInventoryVersion,
            long lastEvaluatedVersion) {
        Objects.requireNonNull(playerUuid, "playerUuid must not be null");
        if (currentInventoryVersion == lastEvaluatedVersion) {
            return false;
        }
        String slotKey = playerUuid + ":" + slotIndex;
        Long cachedVersion = evaluatedSlotVersions.put(slotKey, currentInventoryVersion);
        return cachedVersion == null || cachedVersion != currentInventoryVersion;
    }
}
