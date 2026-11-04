package net.minecraftforge.server.permission.nodes;

import java.util.Objects;

/**
 * Key identifier for dynamic permission evaluation contexts.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @param <T> context payload type
 */
public final class PermissionDynamicContextKey<T> {

    private final Class<T> type;
    private final String name;

    public PermissionDynamicContextKey(Class<T> type, String name) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    public Class<T> getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
