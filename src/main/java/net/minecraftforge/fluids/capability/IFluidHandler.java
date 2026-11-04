package net.minecraftforge.fluids.capability;

import net.minecraftforge.fluids.FluidStack;

/**
 * Universal multi-era capability interface for handling fluids in Forge containers.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public interface IFluidHandler {

    /**
     * Returns the number of fluid tanks available.
     *
     * @return tank count
     */
    int getTanks();

    /**
     * Returns the fluid stack stored in the given tank index.
     *
     * @param tank target tank index
     * @return fluid stack in tank
     */
    FluidStack getFluidInTank(int tank);

    /**
     * Returns the maximum capacity of the given tank index.
     *
     * @param tank target tank index
     * @return tank capacity in millibuckets
     */
    int getTankCapacity(int tank);

    /**
     * Returns whether the given fluid stack is valid for the given tank.
     *
     * @param tank  target tank index
     * @param stack fluid stack to test
     * @return {@code true} if valid
     */
    boolean isFluidValid(int tank, FluidStack stack);

    /**
     * Fills fluid into internal tanks.
     *
     * @param resource resource fluid stack to fill
     * @param action   simulation or execution mode
     * @return amount of fluid actually filled
     */
    int fill(FluidStack resource, IFluidHandler$FluidAction action);

    /**
     * Drains fluid out of internal tanks.
     *
     * @param resource resource fluid stack to drain
     * @param action   simulation or execution mode
     * @return drained fluid stack
     */
    FluidStack drain(FluidStack resource, IFluidHandler$FluidAction action);

    /**
     * Drains fluid out of internal tanks up to maxDrain volume.
     *
     * @param maxDrain maximum volume to drain in millibuckets
     * @param action   simulation or execution mode
     * @return drained fluid stack
     */
    FluidStack drain(int maxDrain, IFluidHandler$FluidAction action);
}
