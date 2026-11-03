package org.bukkit.inventory;

import java.util.Arrays;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryType;

/**
 * Runtime representation of a slot-indexed item container ({@code org.bukkit.inventory.Inventory})
 * on the VESSEL Sovereign Microkernel.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public class Inventory {

    private final InventoryHolder holder;
    private final InventoryType type;
    private final int size;
    private final String title;
    private final ItemStack[] slots;

    /**
     * Constructs an {@code Inventory} with an explicit holder, type, slot capacity, and title.
     *
     * @param holder owning inventory holder (nullable)
     * @param type   non-null inventory type
     * @param size   number of inventory slots (must be {@code > 0})
     * @param title  inventory title
     */
    public Inventory(InventoryHolder holder, InventoryType type, int size, String title) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive: " + size);
        }
        this.holder = holder;
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.size = size;
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.slots = new ItemStack[size];
    }

    /**
     * Constructs a chest-typed {@code Inventory} with an explicit holder, slot capacity, and title.
     *
     * @param holder owning inventory holder (nullable)
     * @param size   number of inventory slots (must be {@code > 0})
     * @param title  inventory title
     */
    public Inventory(InventoryHolder holder, int size, String title) {
        this(holder, InventoryType.CHEST, size, title);
    }

    /**
     * Constructs an {@code Inventory} using the default size and title of the given {@link InventoryType}.
     *
     * @param holder owning inventory holder (nullable)
     * @param type   non-null inventory type
     */
    public Inventory(InventoryHolder holder, InventoryType type) {
        this(
                holder,
                Objects.requireNonNull(type, "type must not be null"),
                type.getDefaultSize(),
                type.getDefaultTitle());
    }

    /**
     * Constructs an {@code Inventory} with the specified slot capacity and title.
     *
     * @param size  number of inventory slots (must be {@code > 0})
     * @param title inventory title
     */
    public Inventory(int size, String title) {
        this(null, InventoryType.CHEST, size, title);
    }

    /**
     * Constructs a chest-titled {@code Inventory} with the specified slot capacity.
     *
     * @param size number of inventory slots (must be {@code > 0})
     */
    public Inventory(int size) {
        this(null, InventoryType.CHEST, size, "Chest");
    }

    public InventoryHolder getHolder() {
        return holder;
    }

    public InventoryType getType() {
        return type;
    }

    public int getSize() {
        return size;
    }

    public String getTitle() {
        return title;
    }

    /**
     * Returns the {@link ItemStack} at the given slot index, or {@code null} if empty.
     *
     * @param index zero-based slot index
     * @return item stack at {@code index}, or {@code null}
     */
    public ItemStack getItem(int index) {
        checkSlotIndex(index);
        return slots[index];
    }

    /**
     * Places the specified {@link ItemStack} into the given slot index.
     *
     * @param index zero-based slot index
     * @param item  item stack to store (nullable)
     */
    public void setItem(int index, ItemStack item) {
        checkSlotIndex(index);
        slots[index] = item;
    }

    /**
     * Places each non-null {@link ItemStack} into the first available empty slot.
     *
     * @param items items to insert
     */
    public void addItem(ItemStack... items) {
        if (items == null) {
            return;
        }
        for (ItemStack item : items) {
            if (item == null) {
                continue;
            }
            for (int i = 0; i < size; i++) {
                if (slots[i] == null) {
                    slots[i] = item;
                    break;
                }
            }
        }
    }

    /**
     * Clears all slots in this inventory.
     */
    public void clear() {
        Arrays.fill(slots, null);
    }

    /**
     * Checks whether at least one slot contains an {@link ItemStack} of the specified {@link Material}.
     *
     * @param material material to match
     * @return {@code true} if present
     */
    public boolean contains(Material material) {
        if (material == null) {
            return false;
        }
        for (ItemStack stack : slots) {
            if (stack != null && stack.getType() == material) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a shallow copy of the underlying slot array.
     *
     * @return cloned array of slot contents
     */
    public ItemStack[] getContents() {
        return slots.clone();
    }

    private void checkSlotIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Slot index out of bounds: " + index + " (size=" + size + ")");
        }
    }
}
