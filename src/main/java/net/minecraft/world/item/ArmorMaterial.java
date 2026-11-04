package net.minecraft.world.item;

/**
 * Universal cross-era interface bridging Minecraft 1.20.1 modded ArmorMaterial implementations
 * to modern Minecraft servers where ArmorMaterial was refactored.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public interface ArmorMaterial {

    int getDurabilityForType(Object type);

    int getDefenseForType(Object type);

    int getEnchantmentValue();

    Object getEquipSound();

    Object getRepairIngredient();

    String getName();

    float getToughness();

    float getKnockbackResistance();
}
