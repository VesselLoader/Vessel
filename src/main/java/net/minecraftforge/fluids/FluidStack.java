package net.minecraftforge.fluids;

import net.minecraft.world.level.material.Fluid;

/**
 * Universal multi-era compatibility representation of a fluid stack (fluid type + amount + tag).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class FluidStack {

    /**
     * Empty fluid stack representation.
     */
    public static final FluidStack EMPTY = new FluidStack(null, 0);

    private final Fluid fluid;
    private int amount;

    /**
     * Constructs a fluid stack with the specified fluid and millibucket volume.
     *
     * @param fluid  fluid type
     * @param amount fluid volume in millibuckets
     */
    public FluidStack(Fluid fluid, int amount) {
        this.fluid = fluid;
        this.amount = Math.max(0, amount);
    }

    /**
     * Returns the fluid type stored in this stack.
     *
     * @return fluid instance, or null if empty
     */
    public Fluid getFluid() {
        return fluid;
    }

    /**
     * Returns the millibucket volume of this fluid stack.
     *
     * @return volume in millibuckets
     */
    public int getAmount() {
        return amount;
    }

    /**
     * Sets the millibucket volume of this fluid stack.
     *
     * @param amount new volume
     */
    public void setAmount(int amount) {
        this.amount = Math.max(0, amount);
    }

    /**
     * Returns whether this fluid stack is empty.
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        return this == EMPTY || fluid == null || amount <= 0;
    }

    /**
     * Creates an identical copy of this fluid stack.
     *
     * @return cloned fluid stack
     */
    public FluidStack copy() {
        if (isEmpty()) {
            return EMPTY;
        }
        return new FluidStack(fluid, amount);
    }
}
