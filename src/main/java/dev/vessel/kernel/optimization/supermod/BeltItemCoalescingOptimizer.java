package dev.vessel.kernel.optimization.supermod;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Coalesces identical items traveling along Create conveyor belts and advances entire belt segments
 * in batch rather than ticking individual belt item slots one by one.
 *
 * @version 1.0.0
 */
public final class BeltItemCoalescingOptimizer {

    private final Map<String, AtomicInteger> beltItemCounts = new ConcurrentHashMap<>();
    private final Map<Long, AtomicInteger> beltProgressOffsets = new ConcurrentHashMap<>();

    /**
     * Coalesces incoming items of the same type onto a Create belt controller.
     *
     * @param beltControllerPos the packed 64-bit belt controller position
     * @param itemId            the canonical item identifier
     * @param incomingCount     the incoming item stack count
     * @return the updated coalesced count for that item on the belt
     */
    public int coalesceItemsOnBelt(long beltControllerPos, String itemId, int incomingCount) {
        Objects.requireNonNull(itemId, "itemId must not be null");
        if (incomingCount <= 0) {
            return 0;
        }
        String key = beltControllerPos + "|" + itemId;
        return beltItemCounts
                .computeIfAbsent(key, ignored -> new AtomicInteger(0))
                .addAndGet(incomingCount);
    }

    /**
     * Advances the transport progress offset of a belt segment batch in a single operation.
     *
     * @param beltControllerPos the packed 64-bit belt controller position
     * @param steps             the number of progress steps to advance
     * @return the updated cumulative belt step offset
     */
    public int advanceBeltSegmentBatch(long beltControllerPos, int steps) {
        if (steps <= 0) {
            AtomicInteger current = beltProgressOffsets.get(beltControllerPos);
            return current != null ? current.get() : 0;
        }
        return beltProgressOffsets
                .computeIfAbsent(beltControllerPos, ignored -> new AtomicInteger(0))
                .addAndGet(steps);
    }
}
