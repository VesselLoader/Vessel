package dev.vessel.kernel.optimization.world;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enforces fair per-player mob caps (Paper/ServerCore style), caches valid spawnable
 * surfaces per chunk section as a 64-word bitset (4096 bits), and throttles custom
 * modded creature spawners (Cobblemon, Pixelmon, Alex's Mobs, In Control!) when local
 * density exceeds 75% of the per-player cap.
 *
 * @version 1.0.0
 */
public final class NaturalSpawnerAndModdedCreatureGovernor {

    private static final double THROTTLE_UTILIZATION_THRESHOLD = 0.75;

    private final Map<Long, long[]> spawnableSurfaceMaskBySection = new ConcurrentHashMap<>();

    /**
     * Creates a new natural spawner and modded creature governor.
     */
    public NaturalSpawnerAndModdedCreatureGovernor() {
    }

    /**
     * Checks whether spawning an additional mob of {@code mobCategory} near {@code playerUuid}
     * is permitted under the per-player mob cap.
     *
     * @param playerUuid        UUID of the nearby player
     * @param mobCategory       mob category name (e.g., {@code "MONSTER"}, {@code "CREATURE"})
     * @param currentLocalCount current number of mobs in this category around the player
     * @param maxPerPlayerCap   maximum allowed mobs in this category per player
     * @return {@code true} if {@code currentLocalCount < maxPerPlayerCap}
     */
    public boolean enforcePerPlayerMobCap(
            UUID playerUuid,
            String mobCategory,
            int currentLocalCount,
            int maxPerPlayerCap) {
        Objects.requireNonNull(playerUuid, "playerUuid must not be null");
        Objects.requireNonNull(mobCategory, "mobCategory must not be null");
        return currentLocalCount < maxPerPlayerCap;
    }

    /**
     * Updates the cached 4096-bit (64 {@code long} words) spawnable surface bitmask for
     * a $16 \times 16 \times 16$ chunk section.
     *
     * @param sectionKey       64-bit packed chunk section key
     * @param validSurfaceMask array of 64-bit bitmask words
     */
    public void updateSpawnableSurfaceBitSet(long sectionKey, long[] validSurfaceMask) {
        Objects.requireNonNull(validSurfaceMask, "validSurfaceMask must not be null");
        spawnableSurfaceMaskBySection.put(
                sectionKey, Arrays.copyOf(validSurfaceMask, validSurfaceMask.length));
    }

    /**
     * Evaluates in $O(1)$ time whether the section-local coordinate {@code (localX, localY, localZ)}
     * is marked as a valid spawnable surface in the cached bitset.
     *
     * @param sectionKey 64-bit packed chunk section key
     * @param localX     section-local X (0..15)
     * @param localY     section-local Y (0..15)
     * @param localZ     section-local Z (0..15)
     * @return {@code true} if the bit is set in the cached surface mask
     */
    public boolean isSurfaceSpawnValid(long sectionKey, int localX, int localY, int localZ) {
        long[] mask = spawnableSurfaceMaskBySection.get(sectionKey);
        if (mask == null || mask.length == 0) {
            return false;
        }
        int bitIndex = ((localY & 15) << 8) | ((localZ & 15) << 4) | (localX & 15);
        int wordIndex = bitIndex >>> 6;
        if (wordIndex >= mask.length) {
            return false;
        }
        return (mask[wordIndex] & (1L << (bitIndex & 63))) != 0L;
    }

    /**
     * Determines whether a modded creature spawner (e.g., Cobblemon, Pixelmon) should
     * skip its heavy biome/NBT spawn scan on {@code currentTick} when local cap utilization
     * is $\ge 75\%$, throttling scans to once every 10 ticks.
     *
     * @param modId                    spawner mod namespace ID
     * @param nearestPlayer            UUID of the nearest player
     * @param localCapUtilizationRatio ratio of active creatures to local cap ($0.0 \dots 1.0$)
     * @param currentTick              current server tick
     * @return {@code true} if the spawner scan should be skipped on this tick
     */
    public boolean shouldThrottleModdedCreatureSpawner(
            String modId,
            UUID nearestPlayer,
            double localCapUtilizationRatio,
            long currentTick) {
        Objects.requireNonNull(modId, "modId must not be null");
        Objects.requireNonNull(nearestPlayer, "nearestPlayer must not be null");
        if (localCapUtilizationRatio < THROTTLE_UTILIZATION_THRESHOLD) {
            return false;
        }
        return (currentTick % 10L) != 0L;
    }
}
