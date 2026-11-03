package org.bukkit.inventory;

import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

/**
 * Represents a furnace smelting recipe ({@code org.bukkit.inventory.FurnaceRecipe}) on the
 * VESSEL Sovereign Microkernel.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class FurnaceRecipe implements Recipe {

    private final NamespacedKey key;
    private final ItemStack result;
    private final Material input;
    private final float experience;
    private final int cookingTime;

    /**
     * Constructs a new {@code FurnaceRecipe}.
     *
     * @param key         non-null recipe key
     * @param result      non-null smelted output item stack
     * @param input       non-null input material
     * @param experience  experience yield upon smelting completion
     * @param cookingTime smelting duration in ticks
     */
    public FurnaceRecipe(
            NamespacedKey key,
            ItemStack result,
            Material input,
            float experience,
            int cookingTime) {
        this.key = Objects.requireNonNull(key, "key must not be null");
        this.result = Objects.requireNonNull(result, "result must not be null");
        this.input = Objects.requireNonNull(input, "input must not be null");
        this.experience = experience;
        this.cookingTime = cookingTime;
    }

    public NamespacedKey getKey() {
        return key;
    }

    @Override
    public ItemStack getResult() {
        return result;
    }

    public Material getInput() {
        return input;
    }

    public float getExperience() {
        return experience;
    }

    public int getCookingTime() {
        return cookingTime;
    }
}
