package net.minecraftforge.items;

import net.minecraft.world.item.ItemStack;

/**
 * Universal multi-era compatibility contract for Forge's {@code IItemHandler} capability.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public interface IItemHandler {

    /**
     * Returns the total number of item slots in this handler.
     *
     * @return slot count
     */
    int getSlots();

    /**
     * Returns the item stack stored in the given slot.
     *
     * @param slot target slot index
     * @return item stack in slot, or empty
     */
    ItemStack getStackInSlot(int slot);

    /**
     * Inserts an item stack into the specified slot.
     *
     * @param slot     target slot index
     * @param stack    item stack to insert
     * @param simulate whether to simulate insertion without mutating state
     * @return remaining uninserted stack
     */
    ItemStack insertItem(int slot, ItemStack stack, boolean simulate);

    /**
     * Extracts an item stack from the specified slot.
     *
     * @param slot     target slot index
     * @param amount   maximum count to extract
     * @param simulate whether to simulate extraction without mutating state
     * @return extracted item stack
     */
    ItemStack extractItem(int slot, int amount, boolean simulate);

    /**
     * Retrieves the maximum stack limit for the specified slot.
     *
     * @param slot target slot index
     * @return maximum item stack capacity
     */
    int getSlotLimit(int slot);

    /**
     * Checks whether the given item stack is valid for the specified slot.
     *
     * @param slot  target slot index
     * @param stack item stack to test
     * @return {@code true} if valid
     */
    boolean isItemValid(int slot, ItemStack stack);
}
