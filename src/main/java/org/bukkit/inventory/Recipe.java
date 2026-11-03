package org.bukkit.inventory;

/**
 * Contract for any crafting or smelting recipe registered on the server
 * ({@code org.bukkit.inventory.Recipe}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public interface Recipe {

    /**
     * Returns the resulting {@link ItemStack} produced by this recipe.
     *
     * @return recipe output item stack
     */
    ItemStack getResult();
}
