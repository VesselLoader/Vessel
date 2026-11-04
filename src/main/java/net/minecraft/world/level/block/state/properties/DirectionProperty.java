package net.minecraft.world.level.block.state.properties;

import net.minecraft.core.Direction;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * Universal cross-era shim for Mojang block state DirectionProperty.
 * Maintained under VESSEL Rule 17: Standalone single-purpose class.
 *
 * @version 1.0.0
 */
public class DirectionProperty extends Property<Direction> {

    private final Collection<Direction> values;

    protected DirectionProperty(String name, Collection<Direction> values) {
        super(name, Direction.class);
        this.values = values;
    }

    public static DirectionProperty create(String name, Predicate<Direction> filter) {
        return new DirectionProperty(name, Arrays.asList(Direction.values()));
    }

    public static DirectionProperty create(String name, Direction... values) {
        return new DirectionProperty(name, Arrays.asList(values));
    }

    public static DirectionProperty create(String name, Collection<Direction> values) {
        return new DirectionProperty(name, values);
    }

    public Collection<Direction> getPossibleValues() {
        return this.values;
    }
}
