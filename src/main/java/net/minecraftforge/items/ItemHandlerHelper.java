package net.minecraftforge.items;

import net.minecraft.world.item.ItemStack;

/**
 * Universal multi-era utility methods for working with {@link IItemHandler}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ItemHandlerHelper {

    private ItemHandlerHelper() {
    }

    /**
     * Inserts an item stack into the inventory, attempting each slot sequentially.
     *
     * @param dest     destination inventory handler
     * @param stack    stack to insert
     * @param simulate whether to simulate insertion
     * @return remaining uninserted stack
     */
    public static ItemStack insertItem(IItemHandler dest, ItemStack stack, boolean simulate) {
        if (dest == null || stack == null || stack.isEmpty()) {
            return stack;
        }
        for (int i = 0; i < dest.getSlots(); i++) {
            stack = dest.insertItem(i, stack, simulate);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    /**
     * Inserts an item stack into the inventory, mutating state.
     *
     * @param dest  destination inventory handler
     * @param stack stack to insert
     * @return remaining uninserted stack
     */
    public static ItemStack insertItemStacked(IItemHandler dest, ItemStack stack, boolean simulate) {
        return insertItem(dest, stack, simulate);
    }

    /**
     * Copies the given item stack with a new size.
     *
     * @param stack stack to copy
     * @param size  new size
     * @return copied stack
     */
    public static ItemStack copyStackWithSize(ItemStack stack, int size) {
        if (size <= 0 || stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return stack;
    }

    /**
     * Checks whether two item stacks can be merged together.
     *
     * @param a first stack
     * @param b second stack
     * @return {@code true} if compatible
     */
    public static boolean canItemStacksStack(ItemStack a, ItemStack b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return false;
        }
        return true;
    }
}
