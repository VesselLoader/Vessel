package org.bukkit.event.inventory;

/**
 * Represents the different types of inventories and container views in Minecraft
 * ({@code org.bukkit.event.inventory.InventoryType}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public enum InventoryType {

    CHEST(27, "Chest"),
    DISPENSER(9, "Dispenser"),
    DROPPER(9, "Dropper"),
    FURNACE(3, "Furnace"),
    WORKBENCH(10, "Crafting"),
    CRAFTING(5, "Crafting"),
    ENCHANTING(2, "Enchant"),
    BREWING(5, "Brewing"),
    PLAYER(36, "Player"),
    ENDER_CHEST(27, "Ender Chest"),
    ANVIL(3, "Repair & Name"),
    HOPPER(5, "Item Hopper"),
    SHULKER_BOX(27, "Shulker Box"),
    BARREL(27, "Barrel");

    private final int defaultSize;
    private final String defaultTitle;

    InventoryType(int defaultSize, String defaultTitle) {
        this.defaultSize = defaultSize;
        this.defaultTitle = defaultTitle;
    }

    /**
     * Returns the default slot capacity for this inventory type.
     *
     * @return default slot count
     */
    public int getDefaultSize() {
        return defaultSize;
    }

    /**
     * Returns the default GUI window title for this inventory type.
     *
     * @return default title string
     */
    public String getDefaultTitle() {
        return defaultTitle;
    }
}
