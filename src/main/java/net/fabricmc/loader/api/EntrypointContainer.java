package net.fabricmc.loader.api;

import java.util.Objects;

/**
 * Canonical Fabric/Quilt entrypoint wrapper pairing an instantiated entrypoint with its
 * owning {@link ModContainer} ({@code net.fabricmc.loader.api.EntrypointContainer}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param <T> entrypoint contract type
 * @version 1.0.0
 */
public final class EntrypointContainer<T> {

    private final T entrypoint;
    private final ModContainer provider;

    /**
     * Constructs an {@code EntrypointContainer} binding the given entrypoint instance and provider mod.
     *
     * @param entrypoint non-null entrypoint instance
     * @param provider   non-null providing {@link ModContainer}
     */
    public EntrypointContainer(T entrypoint, ModContainer provider) {
        this.entrypoint = Objects.requireNonNull(entrypoint, "entrypoint must not be null");
        this.provider = Objects.requireNonNull(provider, "provider must not be null");
    }

    /**
     * Returns the instantiated entrypoint object.
     *
     * @return entrypoint instance
     */
    public T getEntrypoint() {
        return entrypoint;
    }

    /**
     * Returns the {@link ModContainer} that provided this entrypoint.
     *
     * @return provider mod container
     */
    public ModContainer getProvider() {
        return provider;
    }
}
