package org.bukkit.plugin;

import java.util.Objects;

/**
 * Represents a service provider registration managed by {@link ServicesManager}
 * ({@code org.bukkit.plugin.RegisteredServiceProvider}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param <T> service interface/contract type
 * @version 1.0.0
 */
public final class RegisteredServiceProvider<T> implements Comparable<RegisteredServiceProvider<?>> {

    private final Class<T> service;
    private final T provider;
    private final Plugin plugin;
    private final ServicePriority priority;

    /**
     * Constructs a new {@code RegisteredServiceProvider}.
     *
     * @param service  non-null service contract class
     * @param provider non-null service implementation instance
     * @param priority non-null registration priority
     * @param plugin   owning plugin (nullable in unit tests)
     */
    public RegisteredServiceProvider(
            Class<T> service,
            T provider,
            ServicePriority priority,
            Plugin plugin) {
        this.service = Objects.requireNonNull(service, "service must not be null");
        this.provider = Objects.requireNonNull(provider, "provider must not be null");
        this.priority = Objects.requireNonNull(priority, "priority must not be null");
        this.plugin = plugin;
    }

    public Class<T> getService() {
        return service;
    }

    public T getProvider() {
        return provider;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public ServicePriority getPriority() {
        return priority;
    }

    @Override
    public int compareTo(RegisteredServiceProvider<?> other) {
        if (other == null) {
            return -1;
        }
        return Integer.compare(other.priority.ordinal(), this.priority.ordinal());
    }
}
