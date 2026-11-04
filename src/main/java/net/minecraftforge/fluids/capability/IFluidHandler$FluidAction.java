package net.minecraftforge.fluids.capability;

/**
 * Universal multi-era enum representing the execution mode for fluid transfer operations.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public enum IFluidHandler$FluidAction {

    /**
     * Executes the fluid transfer and mutates state.
     */
    EXECUTE,

    /**
     * Simulates the fluid transfer without mutating state.
     */
    SIMULATE;

    /**
     * Returns whether this action represents an execution mode.
     *
     * @return {@code true} if execute
     */
    public boolean execute() {
        return this == EXECUTE;
    }

    /**
     * Returns whether this action represents a simulation mode.
     *
     * @return {@code true} if simulate
     */
    public boolean simulate() {
        return this == SIMULATE;
    }
}
