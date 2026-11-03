package org.bukkit.inventory;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

/**
 * Represents a shapeless crafting recipe ({@code org.bukkit.inventory.ShapelessRecipe}) on the
 * VESSEL Sovereign Microkernel.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ShapelessRecipe implements Recipe {

    private final NamespacedKey key;
    private final ItemStack result;
    private final List<Material> ingredients = new CopyOnWriteArrayList<>();

    /**
     * Constructs a new {@code ShapelessRecipe} with a unique {@link NamespacedKey} and output {@link ItemStack}.
     *
     * @param key    non-null recipe key
     * @param result non-null output item stack
     */
    public ShapelessRecipe(NamespacedKey key, ItemStack result) {
        this.key = Objects.requireNonNull(key, "key must not be null");
        this.result = Objects.requireNonNull(result, "result must not be null");
    }

    public NamespacedKey getKey() {
        return key;
    }

    @Override
    public ItemStack getResult() {
        return result;
    }

    /**
     * Adds a single instance of the specified {@link Material} ingredient.
     *
     * @param ingredient non-null ingredient material
     * @return this {@code ShapelessRecipe}
     */
    public ShapelessRecipe addIngredient(Material ingredient) {
        return addIngredient(1, ingredient);
    }

    /**
     * Adds {@code count} instances of the specified {@link Material} ingredient.
     *
     * @param count      number of ingredients to add (must be {@code >= 1})
     * @param ingredient non-null ingredient material
     * @return this {@code ShapelessRecipe}
     */
    public ShapelessRecipe addIngredient(int count, Material ingredient) {
        Objects.requireNonNull(ingredient, "ingredient must not be null");
        if (count < 1) {
            throw new IllegalArgumentException("count must be at least 1: " + count);
        }
        for (int i = 0; i < count; i++) {
            ingredients.add(ingredient);
        }
        return this;
    }

    public List<Material> getIngredientList() {
        return Collections.unmodifiableList(ingredients);
    }
}
