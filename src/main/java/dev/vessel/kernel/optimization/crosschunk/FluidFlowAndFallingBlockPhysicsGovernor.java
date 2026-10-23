package dev.vessel.kernel.optimization.crosschunk;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches terrain slope fluid flow directions to avoid repeated depth-4 pathfinding,
 * collapses large contiguous columns of falling sand/gravel ($> 32$ blocks) directly
 * inside the chunk palette with zero {@code FallingBlockEntity} spawns, and coalesces
 * bulk fluid pump block updates (Create Hose Pulley, Mekanism Fluidic Plenisher, RFTools).
 *
 * @version 1.0.0
 */
public final class FluidFlowAndFallingBlockPhysicsGovernor {

    private static final int INSTANT_COLLAPSE_THRESHOLD = 32;

    private final Map<String, Byte> slopeFlowCache = new ConcurrentHashMap<>();

    /**
     * Creates a new fluid flow and falling block physics governor.
     */
    public FluidFlowAndFallingBlockPhysicsGovernor() {
    }

    /**
     * Caches the computed 4-bit cardinal slope flow direction mask at {@code (chunkPos, localPos)}.
     *
     * @param chunkPos    64-bit packed chunk coordinate
     * @param localPos    packed section-local coordinate
     * @param flowDirMask 4-bit cardinal flow direction mask
     */
    public void cacheSlopeFlowDirection(long chunkPos, int localPos, byte flowDirMask) {
        slopeFlowCache.put(chunkPos + ":" + localPos, flowDirMask);
    }

    /**
     * Looks up the cached cardinal slope flow direction mask at {@code (chunkPos, localPos)}.
     *
     * @param chunkPos 64-bit packed chunk coordinate
     * @param localPos packed section-local coordinate
     * @return cached flow direction mask, or {@code (byte) 0} if not cached
     */
    public byte lookupCachedFlowDirection(long chunkPos, int localPos) {
        return slopeFlowCache.getOrDefault(chunkPos + ":" + localPos, (byte) 0);
    }

    /**
     * Determines whether a falling sand/gravel column at {@code (chunkPos, x, z)} exceeds
     * the 32-block threshold and should be collapsed directly in the palette without
     * spawning individual {@code FallingBlockEntity} instances.
     *
     * @param chunkPos                64-bit packed chunk coordinate
     * @param x                       column X coordinate
     * @param z                       column Z coordinate
     * @param contiguousFallingBlocks number of contiguous falling blocks in the column
     * @return {@code true} if {@code contiguousFallingBlocks > 32}
     */
    public boolean shouldCollapseColumnInstantly(
            long chunkPos, int x, int z, int contiguousFallingBlocks) {
        return contiguousFallingBlocks > INSTANT_COLLAPSE_THRESHOLD;
    }

    /**
     * Directly shifts a falling column of height {@code columnHeight} from {@code startY}
     * down to {@code landingY} inside a 1D column state array, clearing the vacated upper
     * positions to air ({@code 0}).
     *
     * @param paletteBlockStates 1D vertical column array of block state IDs indexed by Y
     * @param startY             original bottom Y index of the falling column
     * @param landingY           target landing bottom Y index (${\le} \text{startY}$)
     * @param columnHeight       number of contiguous blocks in the falling column
     * @return number of blocks shifted directly in the column array
     */
    public int collapseFallingColumnDirectly(
            int[] paletteBlockStates, int startY, int landingY, int columnHeight) {
        Objects.requireNonNull(paletteBlockStates, "paletteBlockStates must not be null");
        if (columnHeight <= 0 || landingY < 0 || startY < landingY) {
            return 0;
        }
        int moved = 0;
        for (int i = 0; i < columnHeight; i++) {
            int srcY = startY + i;
            int dstY = landingY + i;
            if (srcY >= paletteBlockStates.length || dstY >= paletteBlockStates.length) {
                break;
            }
            if (dstY != srcY) {
                paletteBlockStates[dstY] = paletteBlockStates[srcY];
                paletteBlockStates[srcY] = 0;
            }
            moved++;
        }
        return moved;
    }

    /**
     * Deduplicates and coalesces bulk fluid pump block modification positions within a
     * chunk into a single multi-block update batch count.
     *
     * @param chunkPos          64-bit packed chunk coordinate
     * @param modifiedPositions array of packed block positions modified by the pump
     * @return count of unique modified block positions in the coalesced batch
     */
    public int coalesceFluidPumpUpdates(long chunkPos, long[] modifiedPositions) {
        Objects.requireNonNull(modifiedPositions, "modifiedPositions must not be null");
        Set<Long> uniquePositions = new HashSet<>(modifiedPositions.length);
        for (long pos : modifiedPositions) {
            uniquePositions.add(pos);
        }
        return uniquePositions.size();
    }
}
