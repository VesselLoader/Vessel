package org.bukkit.plugin;

/**
 * Standard Bukkit {@code ServicesManager} contract ({@code org.bukkit.plugin.ServicesManager}),
 * enabling cross-plugin service registration and discovery (such as Vault economy/permission bridges).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public interface ServicesManager {

    <T> void register(
            Class<T> service,
            T provider,
            Plugin plugin,
            ServicePriority priority);

    void unregisterAll(Plugin plugin);

    void unregister(Class<?> service, Object provider);

    <T> T load(Class<T> service);

    <T> RegisteredServiceProvider<T> getRegistration(Class<T> service);

    <T> boolean isProvidedFor(Class<T> service);

    void clear();
}
