package net.minecraft.world.level.block.state.properties;

import java.util.Objects;

/**
 * Universal cross-era shim for Mojang block state Property base.
 * Maintained under VESSEL Rule 17: Standalone single-purpose class.
 *
 * @param <T> property value type
 * @version 1.0.0
 */
public abstract class Property<T extends Comparable<T>> {

    private final String name;
    private final Class<T> valueClass;

    protected Property(String name, Class<T> valueClass) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.valueClass = Objects.requireNonNull(valueClass, "valueClass must not be null");
    }

    public String getName() {
        return this.name;
    }

    public Class<T> getValueClass() {
        return this.valueClass;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Property<?> other)) {
            return false;
        }
        return this.valueClass.equals(other.valueClass) && this.name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return 31 * this.valueClass.hashCode() + this.name.hashCode();
    }
}
