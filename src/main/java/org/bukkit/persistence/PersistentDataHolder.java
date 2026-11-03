package org.bukkit.persistence;

/**
 * Contract for any Bukkit object (such as {@link org.bukkit.inventory.meta.ItemMeta}, entities,
 * or tile states) that carries a {@link PersistentDataContainer}
 * ({@code org.bukkit.persistence.PersistentDataHolder}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public interface PersistentDataHolder {

    /**
     * Returns the custom {@link PersistentDataContainer} bound to this holder.
     *
     * @return persistent data container
     */
    PersistentDataContainer getPersistentDataContainer();
}
