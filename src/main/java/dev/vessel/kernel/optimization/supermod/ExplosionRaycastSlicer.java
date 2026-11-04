package dev.vessel.kernel.optimization.supermod;

import dev.vessel.kernel.gpu.GpuComputeDispatcher;
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
 * <p>
 * Directly integrates with the sovereign GPU/SIMD hardware accelerator to compute spherical
 * raycast trajectories across thousands of rays in parallel before tick slicing.
 * </p>
 *
 * @version 1.0.2
 * @author VESSEL Engine Team
 */
public final class ExplosionRaycastSlicer {

    private final Map<Long, Queue<Long>> explosionQueues = new ConcurrentHashMap<>();
    private final Map<Long, Integer> explosionSliceLimits = new ConcurrentHashMap<>();

    /**
     * Creates a new explosion raycast slicer instance.
     */
    public ExplosionRaycastSlicer() {
    }

    /**
     * Dispatches explosion raycasting to the hardware accelerator (iGPU, dGPU, or CPU SIMD).
     *
     * @param originX  explosion epicenter X
     * @param originY  explosion epicenter Y
     * @param originZ  explosion epicenter Z
     * @param power    explosion force magnitude
     * @param rayCount total rays to cast (e.g. 1357 rays for 16x16x16 spherical boundary)
     * @return array of packed 64-bit affected block positions
     */
    public long[] computeRaycastsAccelerated(
            double originX, double originY, double originZ, float power, int rayCount) {
        return GpuComputeDispatcher.getActiveDispatcher().dispatchExplosionRaycasts(
                originX, originY, originZ, power, rayCount);
    }

    /**
     * Computes spherical raycast trajectories using hardware acceleration and enqueues the resulting
     * affected block coordinates for multi-tick sliced processing.
     *
     * @param explosionId      unique explosion identifier
     * @param originX          explosion epicenter X
     * @param originY          explosion epicenter Y
     * @param originZ          explosion epicenter Z
     * @param power            explosion force magnitude
     * @param rayCount         number of rays to cast
     * @param maxBlocksPerTick maximum blocks permitted to destroy per server tick
     */
    public void enqueueAcceleratedExplosion(
            long explosionId,
            double originX, double originY, double originZ,
            float power, int rayCount,
            int maxBlocksPerTick) {
        long[] affected = computeRaycastsAccelerated(originX, originY, originZ, power, rayCount);
        List<Long> blockList = new ArrayList<>(affected.length);
        for (long pos : affected) {
            blockList.add(pos);
        }
        enqueueMassiveExplosion(explosionId, blockList, maxBlocksPerTick);
    }

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

    /**
     * Checks if an explosion has pending blocks remaining to be sliced.
     *
     * @param explosionId the unique explosion identifier
     * @return true if blocks remain
     */
    public boolean hasPendingSlices(long explosionId) {
        Queue<Long> queue = explosionQueues.get(explosionId);
        return queue != null && !queue.isEmpty();
    }

    /**
     * Cancels any pending blocks for the given explosion.
     *
     * @param explosionId the unique explosion identifier
     */
    public void cancelExplosion(long explosionId) {
        explosionQueues.remove(explosionId);
        explosionSliceLimits.remove(explosionId);
    }

    /**
     * Clears all pending explosions.
     */
    public void clearAll() {
        explosionQueues.clear();
        explosionSliceLimits.clear();
    }
}
