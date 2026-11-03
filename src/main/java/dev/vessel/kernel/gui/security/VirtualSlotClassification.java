package dev.vessel.kernel.gui.security;

/**
 * Functional classification of container inventory slots within the VESSEL Virtual GUI subsystem.
 * Distinguishes between untouchable virtual UI elements, modded machinery interaction slots,
 * and standard player backpack storage.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum VirtualSlotClassification {

    /**
     * Interactive virtual button (e.g., page navigation, machine toggle, confirmation trigger).
     * Item cannot be moved or collected under any circumstances. Clicks only trigger action callbacks.
     */
    BUTTON,

    /**
     * Visual progress bar or energy/fluid indicator gauge.
     * Completely inert to all player interactions.
     */
    PROGRESS_BAR,

    /**
     * Decorative border, separator, or negative-space font background canvas.
     * Completely inert to all player interactions.
     */
    DECORATIVE_FRAME,

    /**
     * Modded machinery input slot (e.g. wheat in Create millstone, raw ore in Mekanism crusher).
     * Accepts valid ingredients; allows extraction of unprocessed items.
     */
    FUNCTIONAL_MOD_INPUT,

    /**
     * Modded machinery transformed output slot (e.g. flour, refined ingots).
     * Allows player extraction only. Insertion or swapping is strictly prohibited.
     */
    FUNCTIONAL_MOD_OUTPUT,

    /**
     * Modded multi-purpose storage inventory slot (e.g. modded chest, drawer, backpack).
     * Delegated to standard item capability handlers.
     */
    FUNCTIONAL_MOD_STORAGE,

    /**
     * Player personal backpack, storage, and hotbar slots (36 standard slots).
     * Free internal movement, monitored during cross-container transfers.
     */
    REAL_PLAYER_BACKPACK;

    /**
     * Checks if this slot represents an untouchable virtual UI element.
     *
     * @return true if the slot is BUTTON, PROGRESS_BAR, or DECORATIVE_FRAME
     */
    public boolean isVirtual() {
        return this == BUTTON || this == PROGRESS_BAR || this == DECORATIVE_FRAME;
    }

    /**
     * Checks if this slot represents a functional modded machinery or storage slot.
     *
     * @return true if the slot belongs to functional mod inputs, outputs, or storage
     */
    public boolean isModFunctional() {
        return this == FUNCTIONAL_MOD_INPUT || this == FUNCTIONAL_MOD_OUTPUT || this == FUNCTIONAL_MOD_STORAGE;
    }

    /**
     * Checks if this slot is part of the player's real personal backpack inventory.
     *
     * @return true if the slot is REAL_PLAYER_BACKPACK
     */
    public boolean isPlayerBackpack() {
        return this == REAL_PLAYER_BACKPACK;
    }
}
