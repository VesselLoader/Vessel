package org.bukkit;

import java.util.Locale;

/**
 * Canonical enumeration of Bukkit/Spigot/Paper block and item materials supported by the VESSEL
 * Sovereign Microkernel runtime.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public enum Material {

    AIR(0, true),
    STONE(64, true),
    GRASS_BLOCK(64, true),
    DIRT(64, true),
    COBBLESTONE(64, true),
    BEDROCK(64, true),
    WATER(1, true),
    LAVA(1, true),
    SAND(64, true),
    GRAVEL(64, true),
    GOLD_ORE(64, true),
    IRON_ORE(64, true),
    COAL_ORE(64, true),
    OAK_LOG(64, true),
    OAK_PLANKS(64, true),
    GLASS(64, true),
    DIAMOND_ORE(64, true),
    DIAMOND_BLOCK(64, true),
    CRAFTING_TABLE(64, true),
    CHEST(64, true),
    FURNACE(64, true),
    OBSIDIAN(64, true),
    REDSTONE(64, true),
    TORCH(64, true),
    BARRIER(64, true),
    DIAMOND_SWORD(1, false),
    DIAMOND_PICKAXE(1, false),
    IRON_INGOT(64, false),
    GOLD_INGOT(64, false),
    DIAMOND(64, false),
    EMERALD(64, false),
    NETHERITE_INGOT(64, false),
    APPLE(64, false),
    GOLDEN_APPLE(64, false);

    private final int maxStackSize;
    private final boolean block;

    Material(int maxStackSize, boolean block) {
        this.maxStackSize = maxStackSize;
        this.block = block;
    }

    /**
     * Returns the canonical {@link NamespacedKey} for this material in the {@code minecraft}
     * namespace.
     *
     * @return the material key
     */
    public NamespacedKey getKey() {
        return NamespacedKey.minecraft(name().toLowerCase(Locale.ROOT));
    }

    /**
     * Gets the maximum amount of this material that can be held in a stack.
     *
     * @return maximum stack size
     */
    public int getMaxStackSize() {
        return maxStackSize;
    }

    /**
     * Checks if this Material is a placable block.
     *
     * @return {@code true} if this material is a block
     */
    public boolean isBlock() {
        return block;
    }

    /**
     * Checks if this Material is an item that can be held in an inventory.
     *
     * @return {@code true} if this material is an item
     */
    public boolean isItem() {
        return this != AIR && this != WATER && this != LAVA;
    }

    /**
     * Checks if this Material is air.
     *
     * @return {@code true} if this material is {@link #AIR}
     */
    public boolean isAir() {
        return this == AIR;
    }

    /**
     * Attempts to match a {@code Material} by its name, stripping any {@code "minecraft:"} prefix
     * and normalizing spaces or hyphens to underscores.
     *
     * @param name material name to match
     * @return matched {@code Material}, or {@code null} if no match exists
     */
    public static Material matchMaterial(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String filtered = name.trim();
        if (filtered.toLowerCase(Locale.ROOT).startsWith("minecraft:")) {
            filtered = filtered.substring("minecraft:".length());
        }
        filtered = filtered.trim()
                .replace(' ', '_')
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);
        try {
            return valueOf(filtered);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
