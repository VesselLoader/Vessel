package dev.vessel.kernel.optimization.supermod;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches Create mod kinetic network speed multipliers per shaft position so RPM propagation across
 * large cogwheel and shaft graphs resolves in {@code O(1)} without recursive graph traversal.
 *
 * <p>Employs flat 64-bit composite primitive hashing to eliminate intermediate map allocations
 * and reduce GC pressure across large kinetic networks.</p>
 *
 * @version 1.0.1
 */
public final class KineticNetworkGraphCache {

    private final Map<Long, Float> shaftSpeedRatios = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> networkKeys = new ConcurrentHashMap<>();

    /**
     * Caches the gear ratio multiplier for a shaft within a kinetic network.
     *
     * @param networkId     the kinetic network identifier
     * @param shaftBlockPos the packed 64-bit shaft block coordinate
     * @param speedRatio    the speed multiplier relative to the network source
     */
    public void cacheNetworkSpeedMultiplier(long networkId, long shaftBlockPos, float speedRatio) {
        long compositeKey = computeKey(networkId, shaftBlockPos);
        shaftSpeedRatios.put(compositeKey, speedRatio);
        networkKeys
                .computeIfAbsent(networkId, ignored -> ConcurrentHashMap.newKeySet())
                .add(compositeKey);
    }

    /**
     * Computes the propagated RPM at a shaft in {@code O(1)} time using the cached speed ratio.
     *
     * @param networkId     the kinetic network identifier
     * @param shaftBlockPos the packed 64-bit shaft block coordinate
     * @param sourceRpm     the source generator RPM
     * @return the propagated RPM, or {@code 0.0f} if the network or shaft is not cached
     */
    public float computePropagatedRpmO1(long networkId, long shaftBlockPos, float sourceRpm) {
        long compositeKey = computeKey(networkId, shaftBlockPos);
        Float ratio = shaftSpeedRatios.get(compositeKey);
        if (ratio == null) {
            return 0.0f;
        }
        return sourceRpm * ratio.floatValue();
    }

    /**
     * Invalidates all cached shaft ratios for a modified kinetic network.
     *
     * @param networkId the kinetic network identifier
     */
    public void invalidateKineticNetwork(long networkId) {
        Set<Long> keys = networkKeys.remove(networkId);
        if (keys != null) {
            for (Long key : keys) {
                shaftSpeedRatios.remove(key);
            }
        }
    }

    /**
     * Clears all cached kinetic networks.
     */
    public void clear() {
        shaftSpeedRatios.clear();
        networkKeys.clear();
    }

    private static long computeKey(long networkId, long shaftPos) {
        return (networkId * 31L) ^ shaftPos;
    }
}
