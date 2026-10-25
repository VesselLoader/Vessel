package dev.vessel.kernel.optimization.world;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coalesces mass item drops (FTB Ultimine, VeinMiner, Apothic Spawners, Mob Grinding Utils)
 * into single virtualized item entities holding up to 65,536 items prior to world spawning,
 * and merges experience orbs within $4 \times 4 \times 4$ spatial cells (Clumps style).
 *
 * @version 1.0.0
 */
public final class ItemAndXpOrbSpatialMergeAccelerator {

    private final Map<String, Integer> virtualItemCountsByCell = new ConcurrentHashMap<>();
    private final Map<Long, Long> mergedXpByCell4x4x4 = new ConcurrentHashMap<>();

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
        String compositeKey = composeCellItemKey(voxelCellHash, itemKey);
        return virtualItemCountsByCell.merge(
                compositeKey,
                Math.min(incomingCount, cap),
                (existing, added) -> Math.min(cap, existing + added));
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
        String compositeKey = composeCellItemKey(voxelCellHash, itemKey);
        int[] extractedHolder = new int[]{0};
        virtualItemCountsByCell.computeIfPresent(compositeKey, (k, current) -> {
            int slice = Math.min(current, maxSlice);
            extractedHolder[0] = slice;
            int remaining = current - slice;
            return remaining > 0 ? remaining : null;
        });
        return extractedHolder[0];
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
            return mergedXpByCell4x4x4.getOrDefault(cell4x4x4Hash, 0L);
        }
        return mergedXpByCell4x4x4.merge(cell4x4x4Hash, (long) incomingXpValue, Long::sum);
    }

    private static String composeCellItemKey(long voxelCellHash, String itemKey) {
        return voxelCellHash + "|" + itemKey.trim().toLowerCase(Locale.ROOT);
    }
}
