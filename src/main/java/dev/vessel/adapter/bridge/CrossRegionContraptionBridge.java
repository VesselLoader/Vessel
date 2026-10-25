package dev.vessel.adapter.bridge;

import dev.vessel.kernel.actor.RegionCoordinateHash;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Synchronizes ownership transfer of dynamic contraptions (Create trains, moving gantries,
 * carriage entities) when crossing $16 \times 16$ chunk Folia regional strand boundaries.
 *
 * @version 1.0.0
 */
public final class CrossRegionContraptionBridge {

    private final Map<Long, Long> contraptionRegionOwners = new ConcurrentHashMap<>();

    /**
     * Creates a new cross-region contraption bridge.
     */
    public CrossRegionContraptionBridge() {
    }

    /**
     * Synchronizes a contraption's movement from {@code (fromChunkX, fromChunkZ)} to
     * {@code (toChunkX, toChunkZ)}, updating its active Folia region strand owner hash.
     *
     * @param contraptionId unique contraption identifier
     * @param fromChunkX    origin chunk X
     * @param fromChunkZ    origin chunk Z
     * @param toChunkX      destination chunk X
     * @param toChunkZ      destination chunk Z
     * @return {@code true} if the contraption crossed into a different Folia region strand
     */
    public boolean synchronizeContraptionBoundaryCrossing(
            long contraptionId, int fromChunkX, int fromChunkZ, int toChunkX, int toChunkZ) {
        long sourceRegionHash = RegionCoordinateHash.fromChunk(fromChunkX, fromChunkZ).toPackedKey();
        long targetRegionHash = RegionCoordinateHash.fromChunk(toChunkX, toChunkZ).toPackedKey();
        contraptionRegionOwners.put(contraptionId, targetRegionHash);
        return sourceRegionHash != targetRegionHash;
    }

    /**
     * Returns the packed 64-bit {@link RegionCoordinateHash} currently owning the given contraption.
     *
     * @param contraptionId unique contraption identifier
     * @return packed region coordinate hash, or {@code 0L} if not tracked
     */
    public long getActiveRegionOwnerHash(long contraptionId) {
        return contraptionRegionOwners.getOrDefault(contraptionId, 0L);
    }
}
