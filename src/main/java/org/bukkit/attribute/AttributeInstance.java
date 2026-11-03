package org.bukkit.attribute;

import java.util.Objects;

/**
 * Canonical Bukkit {@code AttributeInstance} handle tracking the mutable base value of an
 * {@link Attribute} on an entity.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class AttributeInstance {

    private final Attribute attribute;
    private volatile double baseValue;

    public AttributeInstance(Attribute attribute, double baseValue) {
        this.attribute = Objects.requireNonNull(attribute, "attribute must not be null");
        this.baseValue = baseValue;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public double getDefaultValue() {
        return attribute.getDefaultValue();
    }

    public double getBaseValue() {
        return baseValue;
    }

    public void setBaseValue(double value) {
        this.baseValue = value;
    }

    public double getValue() {
        return baseValue;
    }
}
