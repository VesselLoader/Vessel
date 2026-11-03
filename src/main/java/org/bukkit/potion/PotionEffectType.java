package org.bukkit.potion;

import java.util.Locale;
import org.bukkit.NamespacedKey;

/**
 * Canonical enumeration of Minecraft status/potion effect types
 * ({@code org.bukkit.potion.PotionEffectType}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public enum PotionEffectType {

    SPEED("speed"),
    SLOWNESS("slowness"),
    HASTE("haste"),
    STRENGTH("strength"),
    INSTANT_HEALTH("instant_health"),
    JUMP_BOOST("jump_boost"),
    REGENERATION("regeneration"),
    RESISTANCE("resistance"),
    FIRE_RESISTANCE("fire_resistance"),
    WATER_BREATHING("water_breathing"),
    INVISIBILITY("invisibility"),
    BLINDNESS("blindness"),
    NIGHT_VISION("night_vision"),
    WEAKNESS("weakness"),
    POISON("poison"),
    WITHER("wither"),
    ABSORPTION("absorption"),
    GLOWING("glowing");

    private final String keyName;
    private final NamespacedKey key;

    PotionEffectType(String keyName) {
        this.keyName = keyName;
        this.key = NamespacedKey.minecraft(keyName);
    }

    /**
     * Returns the canonical {@link NamespacedKey} of this potion effect type.
     *
     * @return namespaced key
     */
    public NamespacedKey getKey() {
        return key;
    }

    /**
     * Returns the legacy Bukkit uppercase name of this potion effect type.
     *
     * @return uppercase effect name
     */
    public String getName() {
        return name();
    }

    /**
     * Resolves a {@code PotionEffectType} by its enum name or Minecraft key (with or without
     * {@code "minecraft:"} prefix).
     *
     * @param name effect name or key
     * @return matched {@code PotionEffectType}, or {@code null} if unknown
     */
    public static PotionEffectType getByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String normalized = name.trim();
        if (normalized.regionMatches(true, 0, "minecraft:", 0, "minecraft:".length())) {
            normalized = normalized.substring("minecraft:".length()).trim();
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        for (PotionEffectType type : values()) {
            if (type.keyName.equals(lower) || type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        return null;
    }
}
