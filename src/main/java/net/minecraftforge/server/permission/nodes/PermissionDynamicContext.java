package net.minecraftforge.server.permission.nodes;

import java.util.Objects;

/**
 * Key-value pair providing contextual information for permission resolution.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @param <T> context payload type
 */
public final class PermissionDynamicContext<T> {

    private final PermissionDynamicContextKey<T> key;
    private final T value;

    public PermissionDynamicContext(PermissionDynamicContextKey<T> key, T value) {
        this.key = Objects.requireNonNull(key, "key must not be null");
        this.value = value;
    }

    public PermissionDynamicContextKey<T> getDynamicContextKey() {
        return key;
    }

    public T getValue() {
        return value;
    }
}
