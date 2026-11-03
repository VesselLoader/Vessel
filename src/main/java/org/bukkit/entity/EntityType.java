package org.bukkit.entity;

import org.bukkit.NamespacedKey;

/**
 * Canonical Bukkit {@code EntityType} enumeration mapping standard Minecraft entity types to their
 * {@link NamespacedKey} identifiers.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public enum EntityType {
    PLAYER("player"),
    ZOMBIE("zombie"),
    SKELETON("skeleton"),
    CREEPER("creeper"),
    SPIDER("spider"),
    ENDERMAN("enderman"),
    VILLAGER("villager"),
    IRON_GOLEM("iron_golem"),
    ENDER_DRAGON("ender_dragon"),
    WITHER("wither"),
    DROPPED_ITEM("item"),
    ARMOR_STAND("armor_stand");

    private final String keyName;
    private final NamespacedKey key;

    EntityType(String keyName) {
        this.keyName = keyName;
        this.key = NamespacedKey.minecraft(keyName);
    }

    public String getKeyName() {
        return keyName;
    }

    public NamespacedKey getKey() {
        return key;
    }
}
