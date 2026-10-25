package dev.vessel.kernel.adapter.neoforge;

/**
 * Priority ordering levels for NeoForge EventBus 8 event subscriber execution.
 * Ordered from highest precedence to lowest.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum NeoForgeEventPriority {

    /**
     * First to execute; ideal for early state inspection or forced cancellations.
     */
    HIGHEST(0),

    /**
     * Executes after HIGHEST, before NORMAL.
     */
    HIGH(1),

    /**
     * Default subscriber execution priority.
     */
    NORMAL(2),

    /**
     * Executes after NORMAL, before LOWEST.
     */
    LOW(3),

    /**
     * Last to execute; ideal for final reactions or cleanup routines.
     */
    LOWEST(4);

    private final int slot;

    NeoForgeEventPriority(int slot) {
        this.slot = slot;
    }

    /**
     * Returns the numeric priority index (0 = highest precedence).
     *
     * @return priority slot
     */
    public int getSlot() {
        return slot;
    }
}
