package net.minecraftforge.fluids.capability;

import net.minecraft.world.item.ItemStack;

/**
 * Universal multi-era capability interface for item stacks that handle fluids.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public interface IFluidHandlerItem extends IFluidHandler {

    /**
     * Returns the container item stack after operations have been performed.
     *
     * @return container item stack
     */
    ItemStack getContainer();
}
