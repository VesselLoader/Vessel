package dev.vessel.kernel.gui.security;

/**
 * High-level classified action intent derived from low-level container click packets.
 * Decouples raw protocol numbers from security validation logic.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum ClickActionIntent {

    /**
     * Standard mouse pickup or place (Left / Right click).
     */
    NORMAL_PICKUP,

    /**
     * Hotbar number key swap (Keys 1..9, button 0..8).
     */
    SWAP_HOTBAR_1_TO_9,

    /**
     * Offhand swap (Key F, button 40).
     */
    SWAP_OFFHAND_KEY_F,

    /**
     * Single item drop (Key Q, button 0).
     */
    DROP_SINGLE_Q,

    /**
     * Stack drop (Ctrl + Key Q, button 1).
     */
    DROP_STACK_CTRL_Q,

    /**
     * Click outside inventory window border (slot -999).
     */
    OUT_OF_BOUNDS_DROP,

    /**
     * Shift-click rapid transfer between top and bottom inventories.
     */
    SHIFT_TRANSFER,

    /**
     * Double-click gather identical items into cursor stack.
     */
    DOUBLE_CLICK_GATHER,

    /**
     * Drag-split item stack across multiple slots.
     */
    DRAG_SPLIT,

    /**
     * Creative middle-click clone.
     */
    CLONE_CREATIVE,

    /**
     * Container window close, disconnect, or client crash.
     */
    WINDOW_CLOSE,

    /**
     * High-frequency burst sorting or automated inventory manipulation injection.
     */
    RAPID_BURST_SORT,

    /**
     * Slot index out of valid container range (negative or overrun).
     */
    SLOT_OVERRUN,

    /**
     * Concurrent multi-threaded state modification race condition.
     */
    CONCURRENT_CONFLICT,

    /**
     * Direct container button payload injection.
     */
    BUTTON_PACKET_INJECTION,

    /**
     * Unrecognized or malformed action intent.
     */
    UNKNOWN
}
