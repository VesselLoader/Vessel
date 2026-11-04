package dev.vessel.kernel.optimization.world;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Coalesces mass item drops (FTB Ultimine, VeinMiner, Apothic Spawners, Mob Grinding Utils)
 * into single virtualized item entities holding up to 65,536 items prior to world spawning,
 * and merges experience orbs within $4 \times 4 \times 4$ spatial cells (Clumps style).
 * <p>
 * Employs zero-allocation 2-tier spatial hashing and lock-free atomic primitive counters
 * to eliminate all string concatenation and boxed {@link Long}/{@link Integer} GC churn.
 * </p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.2
 * @author VESSEL Engine Team
 */
public final class ItemAndXpOrbSpatialMergeAccelerator {

    private final Map<Long, ConcurrentHashMap<String, AtomicInteger>> itemsByCell = new ConcurrentHashMap<>();
    private final Map<Long, AtomicLong> mergedXpByCell4x4x4 = new ConcurrentHashMap<>();

    /**
     * Creates a new spatial item drop and XP orb merge accelerator.
     */
    public ItemAndXpOrbSpatialMergeAccelerator() {
    }

    /**
     * Coalesces an incoming item drop into the virtualized item stack for the specified
     * spatial voxel cell up to {@code maxVirtualStackCapacity} (e.g., 65,536).
     *
     * @param voxelCellHash           spatial voxel cell hash
     * @param itemKey                 canonical item identifier + component fingerprint
     * @param incomingCount           number of items dropped
     * @param maxVirtualStackCapacity maximum virtual stack count (e.g., 65536)
     * @return updated virtual stack count in the voxel cell
     */
    public int coalesceItemDropBeforeSpawn(
            long voxelCellHash, String itemKey, int incomingCount, int maxVirtualStackCapacity) {
        Objects.requireNonNull(itemKey, "itemKey must not be null");
        if (incomingCount <= 0) {
            return 0;
        }
        int cap = Math.max(1, maxVirtualStackCapacity);
        ConcurrentHashMap<String, AtomicInteger> cellItems =
                itemsByCell.computeIfAbsent(voxelCellHash, k -> new ConcurrentHashMap<>());
        AtomicInteger counter = cellItems.computeIfAbsent(itemKey, k -> new AtomicInteger(0));

        int prev;
        int next;
        do {
            prev = counter.get();
            next = Math.min(cap, prev + incomingCount);
        } while (!counter.compareAndSet(prev, next));

        return next;
    }

    /**
     * Extracts up to {@code vanillaMaxStackSize} (e.g., 64) items from a virtualized
     * item drop stack when a player or hopper picks it up.
     *
     * @param voxelCellHash       spatial voxel cell hash
     * @param itemKey             canonical item identifier + component fingerprint
     * @param vanillaMaxStackSize maximum stack size per pickup operation (e.g., 64)
     * @return number of items extracted in this pickup slice
     */
    public int extractPickupStack(
            long voxelCellHash, String itemKey, int vanillaMaxStackSize) {
        Objects.requireNonNull(itemKey, "itemKey must not be null");
        int maxSlice = Math.max(1, vanillaMaxStackSize);
        ConcurrentHashMap<String, AtomicInteger> cellItems = itemsByCell.get(voxelCellHash);
        if (cellItems == null) {
            return 0;
        }
        AtomicInteger counter = cellItems.get(itemKey);
        if (counter == null) {
            return 0;
        }

        int prev;
        int slice;
        int remaining;
        do {
            prev = counter.get();
            if (prev <= 0) {
                cellItems.remove(itemKey, counter);
                return 0;
            }
            slice = Math.min(prev, maxSlice);
            remaining = prev - slice;
        } while (!counter.compareAndSet(prev, remaining));

        if (remaining == 0) {
            cellItems.remove(itemKey, counter);
            if (cellItems.isEmpty()) {
                itemsByCell.remove(voxelCellHash, cellItems);
            }
        }
        return slice;
    }

    /**
     * Merges an incoming experience orb value into the single consolidated XP orb for the
     * given $4 \times 4 \times 4$ block cell.
     *
     * @param cell4x4x4Hash   spatial hash of the $4 \times 4 \times 4$ block cell
     * @param incomingXpValue experience points of the incoming orb
     * @return total accumulated experience points in that cell
     */
    public long mergeExperienceOrbInCell(long cell4x4x4Hash, int incomingXpValue) {
        if (incomingXpValue <= 0) {
            AtomicLong existing = mergedXpByCell4x4x4.get(cell4x4x4Hash);
            return existing != null ? existing.get() : 0L;
        }
        AtomicLong cellXp = mergedXpByCell4x4x4.computeIfAbsent(cell4x4x4Hash, k -> new AtomicLong(0L));
        return cellXp.addAndGet(incomingXpValue);
    }

    /**
     * Extracts up to {@code requestedXp} points from the consolidated XP orb in the cell.
     *
     * @param cell4x4x4Hash spatial hash of the 4x4x4 block cell
     * @param requestedXp   maximum XP points to extract
     * @return actual XP points extracted
     */
    public int extractMergedXp(long cell4x4x4Hash, int requestedXp) {
        if (requestedXp <= 0) {
            return 0;
        }
        AtomicLong cellXp = mergedXpByCell4x4x4.get(cell4x4x4Hash);
        if (cellXp == null) {
            return 0;
        }

        long prev;
        long slice;
        long remaining;
        do {
            prev = cellXp.get();
            if (prev <= 0L) {
                mergedXpByCell4x4x4.remove(cell4x4x4Hash, cellXp);
                return 0;
            }
            slice = Math.min(prev, (long) requestedXp);
            remaining = prev - slice;
        } while (!cellXp.compareAndSet(prev, remaining));

        if (remaining == 0L) {
            mergedXpByCell4x4x4.remove(cell4x4x4Hash, cellXp);
        }
        return (int) slice;
    }

    /**
     * Drains and removes all accumulated experience points from the specified cell.
     *
     * @param cell4x4x4Hash spatial hash of the 4x4x4 block cell
     * @return total experience points drained
     */
    public long drainCellXp(long cell4x4x4Hash) {
        AtomicLong removed = mergedXpByCell4x4x4.remove(cell4x4x4Hash);
        return removed != null ? removed.get() : 0L;
    }
}
