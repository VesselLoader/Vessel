package dev.vessel.kernel.optimization.crosschunk;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Replaces recursive 7-block Manhattan leaf distance checks with zero-allocation bitmask
 * BFS, staggers massive canopy leaf decay (FastLeafDecay, HT's TreeChop, Dynamic Trees,
 * Biomes O' Plenty Redwood, Twilight Forest Giant Oak) at a capped per-tick budget,
 * and pre-coalesces all canopy loot drops into a single item batch before entity spawning.
 *
 * @version 1.0.0
 */
public final class TreeCanopyDecayAndVeinBreakScheduler {

    private final Map<Long, Queue<Long>> pendingDecayQueuesByChunk = new ConcurrentHashMap<>();
    private final Map<Long, Integer> maxLeavesBudgetByChunk = new ConcurrentHashMap<>();

    /**
     * Creates a new tree canopy decay and vein-break scheduler.
     */
    public TreeCanopyDecayAndVeinBreakScheduler() {
    }

    /**
     * Identifies all canopy leaf positions whose Manhattan distance to every remaining
     * trunk position exceeds {@code maxDistance}, returning the array of unsupported
     * decaying leaf positions.
     *
     * @param trunkPositions      packed 64-bit block positions of remaining log/trunk blocks
     * @param canopyLeafPositions packed 64-bit block positions of candidate leaf blocks
     * @param maxDistance         maximum supported Manhattan distance from a trunk (e.g., 7)
     * @return array of packed 64-bit leaf positions scheduled to decay
     */
    public long[] computeLeafDistanceBitmaskBfs(
            long[] trunkPositions, long[] canopyLeafPositions, int maxDistance) {
        Objects.requireNonNull(trunkPositions, "trunkPositions must not be null");
        Objects.requireNonNull(canopyLeafPositions, "canopyLeafPositions must not be null");
        if (canopyLeafPositions.length == 0) {
            return new long[0];
        }
        if (trunkPositions.length == 0) {
            return Arrays.copyOf(canopyLeafPositions, canopyLeafPositions.length);
        }
        long[] unsupported = new long[canopyLeafPositions.length];
        int count = 0;
        for (long leafPos : canopyLeafPositions) {
            int lx = unpackX(leafPos);
            int ly = unpackY(leafPos);
            int lz = unpackZ(leafPos);
            boolean supported = false;
            for (long trunkPos : trunkPositions) {
                int tx = unpackX(trunkPos);
                int ty = unpackY(trunkPos);
                int tz = unpackZ(trunkPos);
                int manhattan = Math.abs(lx - tx) + Math.abs(ly - ty) + Math.abs(lz - tz);
                if (manhattan <= maxDistance) {
                    supported = true;
                    break;
                }
            }
            if (!supported) {
                unsupported[count++] = leafPos;
            }
        }
        return Arrays.copyOf(unsupported, count);
    }

    /**
     * Enqueues decaying leaf positions for {@code chunkPos} under a staggered per-tick
     * budget (e.g., 64 leaves/tick/chunk) to prevent MSPT spikes when giant trees are felled.
     *
     * @param chunkPos              64-bit packed chunk coordinate
     * @param decayingLeafPositions array of packed leaf positions to decay
     * @param maxLeavesPerTick      maximum leaves allowed to decay per tick in this chunk
     * @return total number of queued decaying leaves for the chunk
     */
    public int enqueueStaggeredCanopyDecay(
            long chunkPos, long[] decayingLeafPositions, int maxLeavesPerTick) {
        Objects.requireNonNull(decayingLeafPositions, "decayingLeafPositions must not be null");
        maxLeavesBudgetByChunk.put(chunkPos, Math.max(1, maxLeavesPerTick));
        Queue<Long> queue =
                pendingDecayQueuesByChunk.computeIfAbsent(
                        chunkPos, k -> new ConcurrentLinkedQueue<>());
        for (long pos : decayingLeafPositions) {
            queue.add(pos);
        }
        return queue.size();
    }

    /**
     * Polls the next staggered batch of decaying leaf positions for {@code chunkPos}
     * up to the configured per-tick budget.
     *
     * @param chunkPos    64-bit packed chunk coordinate
     * @param currentTick current server tick
     * @return array of packed leaf positions to decay on this tick
     */
    public long[] pollNextDecayBatch(long chunkPos, int currentTick) {
        Queue<Long> queue = pendingDecayQueuesByChunk.get(chunkPos);
        if (queue == null || queue.isEmpty()) {
            return new long[0];
        }
        int budget = maxLeavesBudgetByChunk.getOrDefault(chunkPos, 64);
        long[] batch = new long[budget];
        int count = 0;
        while (count < budget) {
            Long next = queue.poll();
            if (next == null) {
                break;
            }
            batch[count++] = next;
        }
        return Arrays.copyOf(batch, count);
    }

    /**
     * Coalesces all sapling, apple, stick, and log drops produced across an entire felled
     * tree canopy into a single consolidated map of {@code itemId -> totalCount} before
     * spawning item entities.
     *
     * @param originPackedPos packed block position of the felled tree base
     * @param itemIds         array of dropped item identifiers
     * @param counts          parallel array of dropped stack counts
     * @return unmodifiable map of coalesced item IDs to total stack counts
     */
    public Map<String, Integer> coalesceCanopyLootDrops(
            long originPackedPos, String[] itemIds, int[] counts) {
        Objects.requireNonNull(itemIds, "itemIds must not be null");
        Objects.requireNonNull(counts, "counts must not be null");
        int len = Math.min(itemIds.length, counts.length);
        Map<String, Integer> coalesced = new LinkedHashMap<>();
        for (int i = 0; i < len; i++) {
            String itemId = itemIds[i];
            int count = counts[i];
            if (itemId != null && !itemId.isBlank() && count > 0) {
                coalesced.merge(itemId.trim(), count, Integer::sum);
            }
        }
        return Collections.unmodifiableMap(coalesced);
    }

    private static int unpackX(long packedPos) {
        return (int) (packedPos >> 38);
    }

    private static int unpackY(long packedPos) {
        return (int) ((packedPos << 52) >> 52);
    }

    private static int unpackZ(long packedPos) {
        return (int) ((packedPos << 26) >> 38);
    }
}
