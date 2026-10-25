package dev.vessel.kernel.optimization.supermod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches Create mod kinetic network speed multipliers per shaft position so RPM propagation across
 * large cogwheel and shaft graphs resolves in {@code O(1)} without recursive graph traversal.
 *
 * @version 1.0.0
 */
public final class KineticNetworkGraphCache {

    private final Map<Long, Map<Long, Float>> networkSpeedRatios = new ConcurrentHashMap<>();

    /**
     * Caches the gear ratio multiplier for a shaft within a kinetic network.
     *
     * @param networkId     the kinetic network identifier
     * @param shaftBlockPos the packed 64-bit shaft block coordinate
     * @param speedRatio    the speed multiplier relative to the network source
     */
    public void cacheNetworkSpeedMultiplier(long networkId, long shaftBlockPos, float speedRatio) {
        networkSpeedRatios
                .computeIfAbsent(networkId, ignored -> new ConcurrentHashMap<>())
                .put(shaftBlockPos, speedRatio);
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
        Map<Long, Float> shafts = networkSpeedRatios.get(networkId);
        if (shafts == null) {
            return 0.0f;
        }
        Float ratio = shafts.get(shaftBlockPos);
        if (ratio == null) {
            return 0.0f;
        }
        return sourceRpm * ratio;
    }

    /**
     * Invalidates all cached shaft ratios for a modified kinetic network.
     *
     * @param networkId the kinetic network identifier
     */
    public void invalidateKineticNetwork(long networkId) {
        networkSpeedRatios.remove(networkId);
    }
}
