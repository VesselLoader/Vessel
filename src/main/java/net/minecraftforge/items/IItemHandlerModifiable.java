package net.minecraftforge.items;

import net.minecraft.world.item.ItemStack;

/**
 * Universal multi-era compatibility contract for Forge's {@code IItemHandlerModifiable} capability.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public interface IItemHandlerModifiable extends IItemHandler {

    /**
     * Overwrites the item stack stored in the given slot directly.
     *
     * @param slot  target slot index
     * @param stack item stack to set
     */
    void setStackInSlot(int slot, ItemStack stack);
}
