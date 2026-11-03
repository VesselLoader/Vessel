package org.bukkit.event.inventory;

/**
 * Represents the type of mouse/keyboard action used to click a slot in an inventory GUI
 * ({@code org.bukkit.event.inventory.ClickType}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public enum ClickType {

    LEFT,
    SHIFT_LEFT,
    RIGHT,
    SHIFT_RIGHT,
    WINDOW_BORDER_LEFT,
    WINDOW_BORDER_RIGHT,
    MIDDLE,
    NUMBER_KEY,
    DOUBLE_CLICK,
    DROP,
    CONTROL_DROP,
    CREATIVE,
    SWAP_OFFHAND,
    UNKNOWN;

    /**
     * Checks whether this click action represents a primary left-click interaction.
     *
     * @return {@code true} if left click
     */
    public boolean isLeftClick() {
        return this == LEFT || this == SHIFT_LEFT || this == DOUBLE_CLICK || this == CREATIVE;
    }

    /**
     * Checks whether this click action represents a secondary right-click interaction.
     *
     * @return {@code true} if right click
     */
    public boolean isRightClick() {
        return this == RIGHT || this == SHIFT_RIGHT;
    }

    /**
     * Checks whether the Shift (or Control-Drop) modifier was held during this click.
     *
     * @return {@code true} if shift click
     */
    public boolean isShiftClick() {
        return this == SHIFT_LEFT || this == SHIFT_RIGHT || this == CONTROL_DROP;
    }
}
