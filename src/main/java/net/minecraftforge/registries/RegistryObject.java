package net.minecraftforge.registries;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Forge deferred registry holder ({@code net.minecraftforge.registries.RegistryObject}) that
 * lazily resolves and caches its registered entry instance during the registry event phase.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param <T> registered entry type
 * @version 1.0.0
 */
public final class RegistryObject<T> implements Supplier<T> {

    private final String id;
    private final Supplier<? extends T> supplier;
    private volatile T value;

    /**
     * Constructs a new {@code RegistryObject} for the specified namespaced identifier and lazy supplier.
     *
     * @param id       namespaced registry entry identifier (e.g., {@code "mymod:custom_block"})
     * @param supplier lazy factory supplier evaluated during registration
     */
    public RegistryObject(String id, Supplier<? extends T> supplier) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.supplier = Objects.requireNonNull(supplier, "supplier must not be null");
    }

    /**
     * Returns the namespaced identifier of this registry object.
     *
     * @return non-null namespaced identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Returns {@code true} if this registry object has been resolved and bound to a non-null value.
     *
     * @return {@code true} if resolved, {@code false} otherwise
     */
    public boolean isPresent() {
        return value != null;
    }

    /**
     * Resolves and caches the underlying entry value from the lazy supplier if not already resolved.
     *
     * @return the resolved entry instance
     */
    public T resolveNow() {
        if (value == null) {
            value = supplier.get();
        }
        return value;
    }

    /**
     * Returns the bound registry entry instance, or throws {@link IllegalStateException} if not yet bound.
     *
     * @return bound registry entry instance
     * @throws IllegalStateException if {@link #isPresent()} is {@code false}
     */
    @Override
    public T get() {
        T current = value;
        if (current == null) {
            throw new IllegalStateException("RegistryObject not bound: " + id);
        }
        return current;
    }

    /**
     * Returns the bound registry entry instance if present, or {@code other} if not yet bound.
     *
     * @param other fallback value
     * @return bound entry or {@code other}
     */
    public T orElse(T other) {
        T current = value;
        return current != null ? current : other;
    }
}
