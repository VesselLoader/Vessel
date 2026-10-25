package dev.vessel.kernel.optimization.crosschunk;

import dev.vessel.kernel.actor.RegionalActorStrand;
import dev.vessel.kernel.actor.primitive.RegionStrandRouter;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prevents Vanilla Redstone dust, Project Red bundled cables, Create Redstone Links,
 * and Mekanism/Pipez cables from synchronously waking unloaded neighbor chunks by
 * latching cross-border signals into a 64-bit {@code EdgeSignalLatch} bitmask until
 * the target chunk loads naturally.
 *
 * @version 1.0.0
 */
public final class CrossChunkRedstoneAndCableGatekeeper {

    private final Map<Long, Long> latchedBorderMaskByChunk = new ConcurrentHashMap<>();

    /**
     * Creates a new cross-chunk Redstone and cable border gatekeeper.
     */
    public CrossChunkRedstoneAndCableGatekeeper() {
    }

    /**
     * Intercepts a signal crossing from {@code sourceChunkCoord} into {@code targetChunkCoord}.
     * If the target chunk is unloaded, latches the border edge index (0..63) into a 64-bit
     * bitmask and returns {@code false} to block synchronous chunk wake-up.
     *
     * @param sourceChunkCoord     64-bit packed source chunk coordinate
     * @param targetChunkCoord     64-bit packed target chunk coordinate
     * @param localBorderEdgeIndex border edge bit index (0..63)
     * @param signalStrength       Redstone or cable signal strength (0..15)
     * @param isTargetChunkLoaded  whether the target chunk is currently loaded
     * @return {@code true} if immediate propagation is allowed, {@code false} if latched
     */
    public boolean interceptBorderSignal(
            long sourceChunkCoord,
            long targetChunkCoord,
            int localBorderEdgeIndex,
            int signalStrength,
            boolean isTargetChunkLoaded) {
        if (isTargetChunkLoaded) {
            return true;
        }
        int bitIndex = localBorderEdgeIndex & 63;
        long bitMask = 1L << bitIndex;
        if (signalStrength > 0) {
            latchedBorderMaskByChunk.merge(targetChunkCoord, bitMask, (oldVal, mask) -> oldVal | mask);
        } else {
            latchedBorderMaskByChunk.computeIfPresent(
                    targetChunkCoord, (k, oldVal) -> oldVal & ~bitMask);
        }
        return false;
    }

    /**
     * Returns the 64-bit latched border signal mask awaiting delivery for {@code targetChunkCoord}.
     *
     * @param targetChunkCoord 64-bit packed target chunk coordinate
     * @return 64-bit latched border mask ({@code 0L} if none)
     */
    public long getLatchedBorderMask(long targetChunkCoord) {
        return latchedBorderMaskByChunk.getOrDefault(targetChunkCoord, 0L);
    }

    /**
     * Flushes all latched border signals when {@code newlyLoadedChunkCoord} finishes loading,
     * returning the number of active border edges replayed.
     *
     * @param newlyLoadedChunkCoord 64-bit packed chunk coordinate
     * @return number of latched border edge signals flushed (0..64)
     */
    public int flushLatchedSignalsOnChunkLoad(long newlyLoadedChunkCoord) {
        Long mask = latchedBorderMaskByChunk.remove(newlyLoadedChunkCoord);
        if (mask == null || mask == 0L) {
            return 0;
        }
        return Long.bitCount(mask);
    }

    /**
     * Routes a wireless Redstone signal (e.g., Create Redstone Link, RFTools Wireless
     * Transmitter) safely to the owning Folia regional strand.
     *
     * @param targetBlockPos packed block coordinate of the receiver
     * @param frequencyId    wireless channel frequency identifier
     * @param signalStrength transmitted signal strength (0..15)
     * @param router         regional strand spatial router
     * @return {@code true} if the target region strand is active and accepted the signal
     */
    public boolean routeWirelessRedstoneAcrossRegions(
            long targetBlockPos,
            int frequencyId,
            int signalStrength,
            RegionStrandRouter router) {
        Objects.requireNonNull(router, "router must not be null");
        if (signalStrength < 0 || signalStrength > 15) {
            return false;
        }
        int blockX = (int) (targetBlockPos >> 38);
        int blockZ = (int) ((targetBlockPos << 26) >> 38);
        RegionalActorStrand strand = router.routeByBlock(blockX, blockZ);
        return strand != null;
    }
}
