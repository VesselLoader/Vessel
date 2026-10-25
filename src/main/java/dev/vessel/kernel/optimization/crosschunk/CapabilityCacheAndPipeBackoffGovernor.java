package dev.vessel.kernel.optimization.crosschunk;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches block capability endpoints ({@code IItemHandler}, {@code IFluidHandler},
 * {@code IEnergyStorage}) invalidated only on block state version changes, and puts
 * extraction pipes (Pipez, Mekanism Logistical Transporters, LaserIO, EnderIO, Hoppers)
 * into $O(1)$ backoff sleep when the destination container is 100% full.
 *
 * @version 1.0.0
 */
public final class CapabilityCacheAndPipeBackoffGovernor {

    private final Map<String, Object> capabilityInstanceCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> capabilityBlockVersionCache = new ConcurrentHashMap<>();
    private final Map<Long, Long> fullContainerInventoryVersions = new ConcurrentHashMap<>();

    /**
     * Creates a new capability cache and pipe backoff governor.
     */
    public CapabilityCacheAndPipeBackoffGovernor() {
    }

    /**
     * Caches a block capability instance at {@code (packedBlockPos, directionOrdinal)}
     * tied to the block's current state version.
     *
     * @param packedBlockPos     64-bit packed block position
     * @param directionOrdinal   direction ordinal (0..5, or -1 for null side)
     * @param capabilityInstance resolved capability handler object
     * @param blockStateVersion  monotonic block state mutation version
     */
    public void cacheBlockCapability(
            long packedBlockPos,
            int directionOrdinal,
            Object capabilityInstance,
            int blockStateVersion) {
        Objects.requireNonNull(capabilityInstance, "capabilityInstance must not be null");
        String key = composeDirectionalKey(packedBlockPos, directionOrdinal);
        capabilityInstanceCache.put(key, capabilityInstance);
        capabilityBlockVersionCache.put(key, blockStateVersion);
    }

    /**
     * Retrieves a cached block capability if the block state version at that position
     * has not changed.
     *
     * @param packedBlockPos           64-bit packed block position
     * @param directionOrdinal         direction ordinal
     * @param currentBlockStateVersion current monotonic block state version
     * @return cached capability instance, or {@code null} if absent or invalidated
     */
    public Object getCachedCapabilityIfValid(
            long packedBlockPos, int directionOrdinal, int currentBlockStateVersion) {
        String key = composeDirectionalKey(packedBlockPos, directionOrdinal);
        Integer cachedVersion = capabilityBlockVersionCache.get(key);
        if (cachedVersion == null || cachedVersion != currentBlockStateVersion) {
            capabilityInstanceCache.remove(key);
            capabilityBlockVersionCache.remove(key);
            return null;
        }
        return capabilityInstanceCache.get(key);
    }

    /**
     * Marks a destination container as 100% full at the specified inventory mutation version.
     *
     * @param packedContainerPos      64-bit packed block position of the destination container
     * @param currentInventoryVersion monotonic version counter of the destination inventory
     */
    public void markDestinationContainerFull(
            long packedContainerPos, long currentInventoryVersion) {
        fullContainerInventoryVersions.put(packedContainerPos, currentInventoryVersion);
    }

    /**
     * Returns {@code true} in $O(1)$ time if the destination container was marked full
     * and its inventory version has not changed since, allowing pipes to skip insertion attempts.
     *
     * @param packedContainerPos      64-bit packed block position of the destination container
     * @param currentInventoryVersion current monotonic inventory version counter
     * @return {@code true} if pipes should skip insertion into this full container
     */
    public boolean shouldPipeSkipFullDestination(
            long packedContainerPos, long currentInventoryVersion) {
        Long fullAtVersion = fullContainerInventoryVersions.get(packedContainerPos);
        if (fullAtVersion == null) {
            return false;
        }
        if (fullAtVersion != currentInventoryVersion) {
            fullContainerInventoryVersions.remove(packedContainerPos);
            return false;
        }
        return true;
    }

    private static String composeDirectionalKey(long packedBlockPos, int directionOrdinal) {
        return packedBlockPos + ":" + directionOrdinal;
    }
}
