package org.bukkit.enchantments;

import java.util.Locale;
import org.bukkit.NamespacedKey;

/**
 * Canonical enumeration of Minecraft item enchantments ({@code org.bukkit.enchantments.Enchantment}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public enum Enchantment {

    PROTECTION("protection", 4),
    FIRE_PROTECTION("fire_protection", 4),
    FEATHER_FALLING("feather_falling", 4),
    BLAST_PROTECTION("blast_protection", 4),
    PROJECTILE_PROTECTION("projectile_protection", 4),
    SHARPNESS("sharpness", 5),
    SMITE("smite", 5),
    BANE_OF_ARTHROPODS("bane_of_arthropods", 5),
    KNOCKBACK("knockback", 2),
    FIRE_ASPECT("fire_aspect", 2),
    LOOTING("looting", 3),
    EFFICIENCY("efficiency", 5),
    SILK_TOUCH("silk_touch", 1),
    UNBREAKING("unbreaking", 3),
    FORTUNE("fortune", 3),
    POWER("power", 5),
    PUNCH("punch", 2),
    FLAME("flame", 1),
    INFINITY("infinity", 1),
    MENDING("mending", 1);

    private final String keyName;
    private final NamespacedKey key;
    private final int maxLevel;

    Enchantment(String keyName, int maxLevel) {
        this.keyName = keyName;
        this.key = NamespacedKey.minecraft(keyName);
        this.maxLevel = maxLevel;
    }

    /**
     * Returns the canonical {@link NamespacedKey} of this enchantment.
     *
     * @return enchantment namespaced key
     */
    public NamespacedKey getKey() {
        return key;
    }

    /**
     * Returns the minimum starting level of this enchantment (`1`).
     *
     * @return starting level
     */
    public int getStartLevel() {
        return 1;
    }

    /**
     * Returns the maximum survival level of this enchantment.
     *
     * @return maximum enchantment level
     */
    public int getMaxLevel() {
        return maxLevel;
    }

    /**
     * Resolves an {@code Enchantment} by its {@link NamespacedKey}.
     *
     * @param key target namespaced key
     * @return matched {@code Enchantment}, or {@code null} if unknown
     */
    public static Enchantment getByKey(NamespacedKey key) {
        if (key == null) {
            return null;
        }
        for (Enchantment enchantment : values()) {
            if (enchantment.key.equals(key)) {
                return enchantment;
            }
        }
        return null;
    }

    /**
     * Resolves an {@code Enchantment} by its enum name or Minecraft key (with or without
     * {@code "minecraft:"} prefix).
     *
     * @param name enchantment name or key
     * @return matched {@code Enchantment}, or {@code null} if unknown
     */
    public static Enchantment getByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String normalized = name.trim();
        if (normalized.regionMatches(true, 0, "minecraft:", 0, "minecraft:".length())) {
            normalized = normalized.substring("minecraft:".length()).trim();
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        for (Enchantment enchantment : values()) {
            if (enchantment.keyName.equals(lower) || enchantment.name().equalsIgnoreCase(normalized)) {
                return enchantment;
            }
        }
        return null;
    }
}
