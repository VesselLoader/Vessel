package org.bukkit.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime implementation of Bukkit's {@code ServicesManager}
 * ({@code org.bukkit.plugin.ServicesManager}), enabling cross-plugin service registration
 * and discovery (such as Vault economy/permission bridges).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ServicesManager {

    private final Map<Class<?>, List<RegisteredServiceProvider<?>>> providers =
            new ConcurrentHashMap<>();

    /**
     * Constructs a new {@code ServicesManager}.
     */
    public ServicesManager() {
    }

    /**
     * Registers a service provider for the given service contract and priority.
     *
     * @param service  non-null service contract class
     * @param provider non-null provider implementation
     * @param plugin   owning plugin (nullable in unit tests)
     * @param priority non-null registration priority
     * @param <T>      service contract type
     */
    public <T> void register(
            Class<T> service,
            T provider,
            Plugin plugin,
            ServicePriority priority) {
        Objects.requireNonNull(service, "service must not be null");
        Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(priority, "priority must not be null");
        RegisteredServiceProvider<T> registration =
                new RegisteredServiceProvider<>(service, provider, priority, plugin);
        providers.compute(service, (key, existing) -> {
            List<RegisteredServiceProvider<?>> updated =
                    existing == null ? new ArrayList<>() : new ArrayList<>(existing);
            updated.add(registration);
            Collections.sort(updated);
            return Collections.unmodifiableList(updated);
        });
    }

    /**
     * Unregisters all service providers owned by the specified plugin.
     *
     * @param plugin target plugin
     */
    public void unregisterAll(Plugin plugin) {
        if (plugin == null) {
            return;
        }
        for (Class<?> service : providers.keySet()) {
            providers.computeIfPresent(service, (key, existing) -> {
                List<RegisteredServiceProvider<?>> remaining = new ArrayList<>();
                for (RegisteredServiceProvider<?> reg : existing) {
                    if (!plugin.equals(reg.getPlugin())) {
                        remaining.add(reg);
                    }
                }
                return remaining.isEmpty() ? null : Collections.unmodifiableList(remaining);
            });
        }
    }

    /**
     * Unregisters a specific service provider instance for the given service class.
     *
     * @param service  service contract class
     * @param provider provider instance to remove
     */
    public void unregister(Class<?> service, Object provider) {
        if (service == null || provider == null) {
            return;
        }
        providers.computeIfPresent(service, (key, existing) -> {
            List<RegisteredServiceProvider<?>> remaining = new ArrayList<>();
            for (RegisteredServiceProvider<?> reg : existing) {
                if (!provider.equals(reg.getProvider())) {
                    remaining.add(reg);
                }
            }
            return remaining.isEmpty() ? null : Collections.unmodifiableList(remaining);
        });
    }

    /**
     * Loads the highest-priority provider instance registered for {@code service}, or {@code null}
     * if none is registered.
     *
     * @param service service contract class
     * @param <T>     service contract type
     * @return highest-priority provider or {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T> T load(Class<T> service) {
        RegisteredServiceProvider<T> registration = getRegistration(service);
        return registration != null ? registration.getProvider() : null;
    }

    /**
     * Returns the highest-priority {@link RegisteredServiceProvider} for {@code service}, or
     * {@code null} if none is registered.
     *
     * @param service service contract class
     * @param <T>     service contract type
     * @return highest-priority registration or {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T> RegisteredServiceProvider<T> getRegistration(Class<T> service) {
        if (service == null) {
            return null;
        }
        List<RegisteredServiceProvider<?>> list = providers.get(service);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return (RegisteredServiceProvider<T>) list.get(0);
    }

    /**
     * Checks whether at least one provider is registered for {@code service}.
     *
     * @param service service contract class
     * @param <T>     service contract type
     * @return {@code true} if provided
     */
    public <T> boolean isProvidedFor(Class<T> service) {
        if (service == null) {
            return false;
        }
        List<RegisteredServiceProvider<?>> list = providers.get(service);
        return list != null && !list.isEmpty();
    }

    /**
     * Clears all service registrations.
     */
    public void clear() {
        providers.clear();
    }
}
