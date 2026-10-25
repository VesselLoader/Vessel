package dev.vessel.kernel.datapack;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Encodes crafting grid items into 64-bit primitive bitmasks for single-cycle CPU matching.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class CraftingGridBitmaskEncoder {

    public static final int MAX_PRIMITIVE_BITS = 64;
    public static final long EMPTY_GRID_MASK = 0L;

    private final Map<String, Integer> itemToBitMap = new ConcurrentHashMap<>();
    private final AtomicInteger bitCounter = new AtomicInteger(0);

    /**
     * Constructs a CraftingGridBitmaskEncoder.
     */
    public CraftingGridBitmaskEncoder() {
    }

    /**
     * Obtains or registers a 64-bit mask for an item ID.
     * Falls back to deterministic hash modulo 64 if unique item types exceed 64.
     *
     * @param itemId namespaced item identifier
     * @return 64-bit bitmask with single bit set
     */
    public long getOrAssignItemMask(String itemId) {
        Objects.requireNonNull(itemId, "itemId cannot be null");
        int bit = itemToBitMap.computeIfAbsent(itemId, id -> {
            int current = bitCounter.getAndIncrement();
            return (current < MAX_PRIMITIVE_BITS) ? current : Math.abs(id.hashCode() % MAX_PRIMITIVE_BITS);
        });
        return 1L << bit;
    }

    /**
     * Encodes an array of grid item IDs into a combined 64-bit bitmask.
     *
     * @param gridItems array of item IDs in crafting matrix
     * @return combined 64-bit bitmask
     */
    public long encodeGrid(String[] gridItems) {
        if (gridItems == null || gridItems.length == 0) {
            return EMPTY_GRID_MASK;
        }

        long mask = 0L;
        for (String item : gridItems) {
            if (item != null && !item.isEmpty() && !item.equals("minecraft:air")) {
                mask |= getOrAssignItemMask(item);
            }
        }
        return mask;
    }

    /**
     * Clears all registered bit assignments.
     */
    public void reset() {
        itemToBitMap.clear();
        bitCounter.set(0);
    }
}
