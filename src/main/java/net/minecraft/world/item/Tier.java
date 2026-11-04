package net.minecraft.world.item;

/**
 * Universal cross-era interface bridging Minecraft 1.20.1 modded tool Tier implementations
 * to modern Minecraft servers where Tier was refactored into ToolMaterial.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public interface Tier {

    int getUses();

    float getSpeed();

    float getAttackDamageBonus();

    int getLevel();

    int getEnchantmentValue();

    Object getRepairIngredient();
}
