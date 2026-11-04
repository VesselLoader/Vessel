package dev.vessel.kernel.world;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Guards multiblock structure integrity across chunk boundaries against premature unloading or desynchronization.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public final class MultiblockStructureIntegrityGuard {

    private final ConcurrentMap<String, Set<Long>> multiblockChunkRegistry = new ConcurrentHashMap<>();

    public void registerMultiblockSpan(String multiblockId, Set<Long> spannedChunkCoordinates) {
        Objects.requireNonNull(multiblockId, "multiblockId cannot be null");
        Objects.requireNonNull(spannedChunkCoordinates, "spannedChunkCoordinates cannot be null");
        multiblockChunkRegistry.put(multiblockId, Set.copyOf(spannedChunkCoordinates));
    }

    public boolean isChunkPartOfActiveMultiblock(long chunkCoordinate) {
        for (Set<Long> coords : multiblockChunkRegistry.values()) {
            if (coords.contains(chunkCoordinate)) {
                return true;
            }
        }
        return false;
    }

    public void unregisterMultiblock(String multiblockId) {
        if (multiblockId != null) {
            multiblockChunkRegistry.remove(multiblockId);
        }
    }

    public int getActiveMultiblockCount() {
        return multiblockChunkRegistry.size();
    }
}
