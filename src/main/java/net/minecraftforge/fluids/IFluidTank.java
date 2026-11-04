package net.minecraftforge.fluids;

import net.minecraftforge.fluids.capability.IFluidHandler$FluidAction;

/**
 * Universal multi-era interface for a single fluid tank.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public interface IFluidTank {

    /**
     * Returns the fluid stored in this tank.
     *
     * @return fluid stack in tank
     */
    FluidStack getFluid();

    /**
     * Returns the amount of fluid stored in this tank.
     *
     * @return fluid amount
     */
    int getFluidAmount();

    /**
     * Returns the maximum capacity of this tank.
     *
     * @return tank capacity
     */
    int getCapacity();

    /**
     * Checks whether the given fluid stack is valid for this tank.
     *
     * @param stack fluid stack to check
     * @return {@code true} if valid
     */
    boolean isFluidValid(FluidStack stack);

    /**
     * Fills fluid into this tank.
     *
     * @param resource fluid to fill
     * @param action   action mode
     * @return amount filled
     */
    int fill(FluidStack resource, IFluidHandler$FluidAction action);

    /**
     * Drains fluid from this tank.
     *
     * @param maxDrain maximum volume to drain
     * @param action   action mode
     * @return drained fluid stack
     */
    FluidStack drain(int maxDrain, IFluidHandler$FluidAction action);

    /**
     * Drains fluid matching resource from this tank.
     *
     * @param resource fluid to drain
     * @param action   action mode
     * @return drained fluid stack
     */
    FluidStack drain(FluidStack resource, IFluidHandler$FluidAction action);
}
