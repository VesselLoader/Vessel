package net.neoforged.neoforge.registries;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * NeoForge lazy registry reference holder ({@code net.neoforged.neoforge.registries.DeferredHolder})
 * that wraps a deferred supplier until registry resolution binds the concrete instance.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @param <R> base registry supertype
 * @param <T> concrete registered entry type
 */
public final class DeferredHolder<R, T extends R> implements Supplier<T> {

    private final String id;
    private final Supplier<? extends T> supplier;
    private volatile T boundValue;

    /**
     * Constructs a new {@code DeferredHolder} for the given namespaced identifier and lazy supplier.
     *
     * @param id       canonical namespaced entry identifier ({@code modid:name})
     * @param supplier lazy factory supplying the entry instance during registration
     */
    public DeferredHolder(String id, Supplier<? extends T> supplier) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.supplier = Objects.requireNonNull(supplier, "supplier must not be null");
    }

    /**
     * Returns the canonical namespaced identifier of this registry entry.
     *
     * @return namespaced entry identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Returns whether this holder's value has been resolved and bound.
     *
     * @return {@code true} if bound
     */
    public boolean isBound() {
        return boundValue != null;
    }

    /**
     * Resolves and binds the entry value immediately if not already bound.
     *
     * @return the bound entry instance
     */
    public T resolveNow() {
        if (boundValue == null) {
            boundValue = supplier.get();
        }
        return boundValue;
    }

    /**
     * Returns the bound registry entry instance, or throws {@link IllegalStateException} if
     * queried before registry binding completes.
     *
     * @return the bound entry instance
     * @throws IllegalStateException if not yet bound
     */
    @Override
    public T get() {
        T current = boundValue;
        if (current == null) {
            throw new IllegalStateException("DeferredHolder not bound: " + id);
        }
        return current;
    }

    /**
     * Alias for {@link #get()} matching NeoForge {@code Holder.value()} conventions.
     *
     * @return the bound entry instance
     */
    public T value() {
        return get();
    }

    /**
     * Returns the bound entry instance if present, or {@code other} if not yet bound.
     *
     * @param other fallback value
     * @return bound value or {@code other}
     */
    public T orElse(T other) {
        T current = boundValue;
        return current != null ? current : other;
    }
}
