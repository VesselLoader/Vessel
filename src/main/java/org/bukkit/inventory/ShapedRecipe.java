package org.bukkit.inventory;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

/**
 * Represents a shaped crafting grid recipe ({@code org.bukkit.inventory.ShapedRecipe}) on the
 * VESSEL Sovereign Microkernel.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ShapedRecipe implements Recipe {

    private final NamespacedKey key;
    private final ItemStack result;
    private String[] rows = new String[0];
    private final Map<Character, Material> ingredientMap = new ConcurrentHashMap<>();

    /**
     * Constructs a new {@code ShapedRecipe} with a unique {@link NamespacedKey} and output {@link ItemStack}.
     *
     * @param key    non-null recipe key
     * @param result non-null output item stack
     */
    public ShapedRecipe(NamespacedKey key, ItemStack result) {
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
     * Configures the 1-to-3 row crafting grid pattern for this shaped recipe.
     *
     * @param shape 1 to 3 non-null pattern rows
     * @return this {@code ShapedRecipe}
     */
    public ShapedRecipe shape(String... shape) {
        Objects.requireNonNull(shape, "shape must not be null");
        if (shape.length < 1 || shape.length > 3) {
            throw new IllegalArgumentException("ShapedRecipe must have 1 to 3 rows, got: " + shape.length);
        }
        for (String row : shape) {
            Objects.requireNonNull(row, "shape row must not be null");
        }
        this.rows = shape.clone();
        return this;
    }

    public String[] getShape() {
        return rows.clone();
    }

    /**
     * Binds a pattern character to the specified {@link Material} ingredient.
     *
     * @param key        pattern character
     * @param ingredient non-null ingredient material
     * @return this {@code ShapedRecipe}
     */
    public ShapedRecipe setIngredient(char key, Material ingredient) {
        ingredientMap.put(key, Objects.requireNonNull(ingredient, "ingredient must not be null"));
        return this;
    }

    public Map<Character, Material> getIngredientMap() {
        return Collections.unmodifiableMap(ingredientMap);
    }
}
