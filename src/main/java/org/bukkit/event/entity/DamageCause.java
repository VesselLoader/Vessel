package org.bukkit.event.entity;

/**
 * Canonical Bukkit {@code DamageCause} enumeration representing the environmental or combat source
 * of an {@link EntityDamageEvent}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public enum DamageCause {
    CONTACT,
    ENTITY_ATTACK,
    ENTITY_SWEEP_ATTACK,
    PROJECTILE,
    SUFFOCATION,
    FALL,
    FIRE,
    FIRE_TICK,
    LAVA,
    DROWNING,
    BLOCK_EXPLOSION,
    ENTITY_EXPLOSION,
    VOID,
    LIGHTNING,
    STARVATION,
    POISON,
    MAGIC,
    WITHER,
    CUSTOM
}
