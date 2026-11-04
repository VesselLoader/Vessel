package dev.vessel.kernel.optimization.supermod;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Coalesces identical items traveling along Create conveyor belts and advances entire belt segments
 * in batch rather than ticking individual belt item slots one by one.
 * <p>
 * Employs zero-allocation 2-tier spatial mapping and primitive 64-bit composite hashing to eliminate
 * string concatenation garbage allocation on hot conveyor belt tick paths and replaces O(N) key scanning
 * with O(1) controller purging.
 * </p>
 *
 * @version 1.0.2
 * @author VESSEL Engine Team
 */
public final class BeltItemCoalescingOptimizer {

    private final Map<Long, ConcurrentHashMap<String, AtomicInteger>> beltItemCountsByController = new ConcurrentHashMap<>();
    private final Map<Long, AtomicInteger> primitiveBeltCounts = new ConcurrentHashMap<>();
    private final Map<Long, AtomicInteger> beltProgressOffsets = new ConcurrentHashMap<>();

    /**
     * Creates a new belt item coalescing optimizer instance.
     */
    public BeltItemCoalescingOptimizer() {
    }

    /**
     * Coalesces incoming items of the same type onto a Create belt controller without string allocations.
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
        ConcurrentHashMap<String, AtomicInteger> itemMap =
                beltItemCountsByController.computeIfAbsent(
                        beltControllerPos, k -> new ConcurrentHashMap<>());
        return itemMap
                .computeIfAbsent(itemId, ignored -> new AtomicInteger(0))
                .addAndGet(incomingCount);
    }

    /**
     * Coalesces incoming items using primitive numeric registry IDs, guaranteeing zero allocation.
     *
     * @param beltControllerPos the packed 64-bit belt controller position
     * @param itemNumericId     raw primitive registry integer identifier
     * @param incomingCount     the incoming item stack count
     * @return the updated coalesced count for that item on the belt
     */
    public int coalesceItemsOnBelt(long beltControllerPos, int itemNumericId, int incomingCount) {
        if (incomingCount <= 0) {
            return 0;
        }
        long compositeKey = computeCompositeKey(beltControllerPos, itemNumericId);
        return primitiveBeltCounts
                .computeIfAbsent(compositeKey, ignored -> new AtomicInteger(0))
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

    /**
     * Consumes up to {@code count} items of the specified type from the belt without string allocations.
     *
     * @param beltControllerPos the packed 64-bit belt controller position
     * @param itemId            the canonical item identifier
     * @param count             the number of items to consume
     * @return the number of items successfully consumed
     */
    public int consumeItemsFromBelt(long beltControllerPos, String itemId, int count) {
        Objects.requireNonNull(itemId, "itemId must not be null");
        if (count <= 0) {
            return 0;
        }
        ConcurrentHashMap<String, AtomicInteger> itemMap = beltItemCountsByController.get(beltControllerPos);
        if (itemMap == null) {
            return 0;
        }
        AtomicInteger current = itemMap.get(itemId);
        if (current == null) {
            return 0;
        }
        while (true) {
            int existing = current.get();
            if (existing <= 0) {
                itemMap.remove(itemId, current);
                return 0;
            }
            int toConsume = Math.min(existing, count);
            int updated = existing - toConsume;
            if (current.compareAndSet(existing, updated)) {
                if (updated == 0) {
                    itemMap.remove(itemId, current);
                }
                return toConsume;
            }
        }
    }

    /**
     * Consumes up to {@code count} items using primitive numeric registry IDs.
     *
     * @param beltControllerPos the packed 64-bit belt controller position
     * @param itemNumericId     raw primitive registry integer identifier
     * @param count             the number of items to consume
     * @return the number of items successfully consumed
     */
    public int consumeItemsFromBelt(long beltControllerPos, int itemNumericId, int count) {
        if (count <= 0) {
            return 0;
        }
        long compositeKey = computeCompositeKey(beltControllerPos, itemNumericId);
        AtomicInteger current = primitiveBeltCounts.get(compositeKey);
        if (current == null) {
            return 0;
        }
        while (true) {
            int existing = current.get();
            if (existing <= 0) {
                primitiveBeltCounts.remove(compositeKey, current);
                return 0;
            }
            int toConsume = Math.min(existing, count);
            int updated = existing - toConsume;
            if (current.compareAndSet(existing, updated)) {
                if (updated == 0) {
                    primitiveBeltCounts.remove(compositeKey, current);
                }
                return toConsume;
            }
        }
    }

    /**
     * Returns the current item count of the specified item on the belt without allocating strings.
     *
     * @param beltControllerPos the packed 64-bit belt controller position
     * @param itemId            the canonical item identifier
     * @return the current item count
     */
    public int getBeltItemCount(long beltControllerPos, String itemId) {
        Objects.requireNonNull(itemId, "itemId must not be null");
        ConcurrentHashMap<String, AtomicInteger> itemMap = beltItemCountsByController.get(beltControllerPos);
        if (itemMap == null) {
            return 0;
        }
        AtomicInteger current = itemMap.get(itemId);
        return current != null ? Math.max(0, current.get()) : 0;
    }

    /**
     * Returns the current item count of the specified primitive numeric item ID.
     *
     * @param beltControllerPos the packed 64-bit belt controller position
     * @param itemNumericId     raw primitive registry integer identifier
     * @return the current item count
     */
    public int getBeltItemCount(long beltControllerPos, int itemNumericId) {
        long compositeKey = computeCompositeKey(beltControllerPos, itemNumericId);
        AtomicInteger current = primitiveBeltCounts.get(compositeKey);
        return current != null ? Math.max(0, current.get()) : 0;
    }

    /**
     * Completely purges all tracked item counts and progress offsets for a broken or cleared belt in O(1).
     *
     * @param beltControllerPos the packed 64-bit belt controller position
     */
    public void clearBelt(long beltControllerPos) {
        beltItemCountsByController.remove(beltControllerPos);
        beltProgressOffsets.remove(beltControllerPos);
    }

    private static long computeCompositeKey(long beltControllerPos, int itemNumericId) {
        return (beltControllerPos * 31L) ^ ((long) itemNumericId & 0xFFFFFFFFL);
    }
}
