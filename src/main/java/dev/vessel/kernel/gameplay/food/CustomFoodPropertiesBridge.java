package dev.vessel.kernel.gameplay.food;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Bridges custom food nutrition, saturation modifier, eating ticks, and status effects
 * from mods into modern Minecraft FoodProperties.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public final class CustomFoodPropertiesBridge {

    private final ConcurrentMap<String, FoodDescriptorRecord> registeredFoods = new ConcurrentHashMap<>();

    public void registerFoodProperties(String itemIdentifier, int nutrition, float saturation, int eatDurationTicks, boolean alwaysEdible) {
        Objects.requireNonNull(itemIdentifier, "itemIdentifier cannot be null");
        FoodDescriptorRecord record = new FoodDescriptorRecord(nutrition, saturation, eatDurationTicks, alwaysEdible);
        registeredFoods.put(itemIdentifier, record);
    }

    public int getNutrition(String itemIdentifier) {
        FoodDescriptorRecord record = registeredFoods.get(itemIdentifier);
        return record != null ? record.nutrition() : 0;
    }

    public float getSaturation(String itemIdentifier) {
        FoodDescriptorRecord record = registeredFoods.get(itemIdentifier);
        return record != null ? record.saturation() : 0.0f;
    }

    public int getEatDurationTicks(String itemIdentifier) {
        FoodDescriptorRecord record = registeredFoods.get(itemIdentifier);
        return record != null ? record.eatDurationTicks() : 32; // Default Minecraft eating ticks
    }

    public boolean isAlwaysEdible(String itemIdentifier) {
        FoodDescriptorRecord record = registeredFoods.get(itemIdentifier);
        return record != null && record.alwaysEdible();
    }

    public boolean isCustomFood(String itemIdentifier) {
        return itemIdentifier != null && registeredFoods.containsKey(itemIdentifier);
    }
}
