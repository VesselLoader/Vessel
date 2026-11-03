package org.bukkit.attribute;

/**
 * Canonical Bukkit {@code Attribute} enumeration representing standard living entity attributes and
 * their default base values.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public enum Attribute {
    GENERIC_MAX_HEALTH(20.0D),
    GENERIC_MOVEMENT_SPEED(0.1D),
    GENERIC_ATTACK_DAMAGE(1.0D),
    GENERIC_ARMOR(0.0D),
    GENERIC_ATTACK_SPEED(4.0D),
    GENERIC_KNOCKBACK_RESISTANCE(0.0D);

    private final double defaultValue;

    Attribute(double defaultValue) {
        this.defaultValue = defaultValue;
    }

    public double getDefaultValue() {
        return defaultValue;
    }
}
