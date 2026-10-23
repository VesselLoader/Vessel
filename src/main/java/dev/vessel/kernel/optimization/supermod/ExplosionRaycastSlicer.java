package dev.vessel.kernel.optimization.supermod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Slices massive Draconic Evolution, Mekanism Reactor, and ICBM explosion block destruction lists
 * across multiple ticks so server MSPT never spikes during catastrophic explosions.
 *
 * @version 1.0.0
 */
public final class ExplosionRaycastSlicer {

    private final Map<Long, Queue<Long>> explosionQueues = new ConcurrentHashMap<>();
    private final Map<Long, Integer> explosionSliceLimits = new ConcurrentHashMap<>();

    /**
     * Enqueues a massive explosion's affected blocks for multi-tick sliced processing.
     *
     * @param explosionId      the unique explosion identifier
     * @param affectedBlocks   the ordered list of packed 64-bit block positions to destroy
     * @param maxBlocksPerTick the maximum number of blocks to process per tick slice
     */
    public void enqueueMassiveExplosion(
            long explosionId,
            List<Long> affectedBlocks,
            int maxBlocksPerTick) {
        Objects.requireNonNull(affectedBlocks, "affectedBlocks must not be null");
        Queue<Long> queue = new ConcurrentLinkedQueue<>(affectedBlocks);
        explosionQueues.put(explosionId, queue);
        explosionSliceLimits.put(explosionId, Math.max(1, maxBlocksPerTick));
    }

    /**
     * Polls the next tick slice of blocks to destroy for the given explosion.
     *
     * @param explosionId the unique explosion identifier
     * @return the list of packed 64-bit block coordinates for this tick slice
     */
    public List<Long> processNextExplosionSlice(long explosionId) {
        Queue<Long> queue = explosionQueues.get(explosionId);
        if (queue == null || queue.isEmpty()) {
            explosionQueues.remove(explosionId);
            explosionSliceLimits.remove(explosionId);
            return List.of();
        }
        int limit = explosionSliceLimits.getOrDefault(explosionId, 256);
        List<Long> slice = new ArrayList<>(Math.min(limit, queue.size()));
        for (int i = 0; i < limit; i++) {
            Long nextBlock = queue.poll();
            if (nextBlock == null) {
                break;
            }
            slice.add(nextBlock);
        }
        if (queue.isEmpty()) {
            explosionQueues.remove(explosionId);
            explosionSliceLimits.remove(explosionId);
        }
        return slice;
    }
}
