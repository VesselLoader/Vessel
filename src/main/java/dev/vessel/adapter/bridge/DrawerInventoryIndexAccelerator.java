package dev.vessel.adapter.bridge;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides an $O(1)$ hash index for Storage Drawers and Functional Storage drawer
 * controllers, replacing linear $O(N)$ slot scans across hundreds of connected drawers
 * during pipe/hopper insertion ticks.
 *
 * @version 1.0.0
 */
public final class DrawerInventoryIndexAccelerator {

    private final Map<Long, Map<String, Integer>> controllerItemSlotIndex =
            new ConcurrentHashMap<>();

    /**
     * Creates a new drawer controller $O(1)$ slot index accelerator.
     */
    public DrawerInventoryIndexAccelerator() {
    }

    /**
     * Binds an item identifier to a specific drawer slot index under a drawer controller.
     *
     * @param controllerPos 64-bit packed block position of the drawer controller
     * @param itemId        namespaced item identifier
     * @param slotIndex     zero-based drawer slot index
     */
    public void bindItemToDrawerSlot(long controllerPos, String itemId, int slotIndex) {
        Objects.requireNonNull(itemId, "itemId must not be null");
        if (slotIndex < 0) {
            throw new IllegalArgumentException("slotIndex must be >= 0");
        }
        Map<String, Integer> itemMap =
                controllerItemSlotIndex.computeIfAbsent(
                        controllerPos, k -> new ConcurrentHashMap<>());
        itemMap.put(itemId.trim().toLowerCase(Locale.ROOT), slotIndex);
    }

    /**
     * Performs an $O(1)$ lookup for the drawer slot index holding {@code itemId} on the
     * given controller.
     *
     * @param controllerPos 64-bit packed block position of the drawer controller
     * @param itemId        namespaced item identifier
     * @return drawer slot index, or {@code -1} if not bound
     */
    public int lookupDrawerSlotO1(long controllerPos, String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return -1;
        }
        Map<String, Integer> itemMap = controllerItemSlotIndex.get(controllerPos);
        if (itemMap == null) {
            return -1;
        }
        return itemMap.getOrDefault(itemId.trim().toLowerCase(Locale.ROOT), -1);
    }

    /**
     * Invalidates the cached slot index for a drawer controller when its multiblock
     * network changes or is broken.
     *
     * @param controllerPos 64-bit packed block position of the drawer controller
     */
    public void invalidateControllerIndex(long controllerPos) {
        controllerItemSlotIndex.remove(controllerPos);
    }
}
