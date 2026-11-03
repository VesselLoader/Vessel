package org.bukkit.inventory;

/**
 * Contract for any object (such as a block state, entity, or custom GUI controller) capable of
 * owning an {@link Inventory} ({@code org.bukkit.inventory.InventoryHolder}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public interface InventoryHolder {

    /**
     * Returns the {@link Inventory} associated with this holder.
     *
     * @return held inventory
     */
    Inventory getInventory();
}
