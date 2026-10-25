package org.bukkit.plugin;

import dev.vessel.adapter.bukkit.PaperPluginManagerShim;
import dev.vessel.kernel.event.EventExecutionPhase;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Runtime Bukkit/Paper {@code PluginManager} implementation backed directly by VESSEL's
 * {@link PaperPluginManagerShim}. Supports plugin lifecycle activation, reflective event handler
 * discovery on {@link Listener} instances, and synchronous/asynchronous event dispatch.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class PluginManager {

    private final PaperPluginManagerShim delegateShim = new PaperPluginManagerShim();
    private final Map<String, Plugin> loadedPlugins = new ConcurrentHashMap<>();

    /**
     * Registers a loaded {@link Plugin} instance with the plugin manager.
     *
     * @param plugin the plugin instance
     */
    public void registerLoadedPlugin(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        loadedPlugins.put(plugin.getName().toLowerCase(), plugin);
    }

    /**
     * Enables a plugin and invokes its {@link Plugin#onEnable()} lifecycle hook.
     *
     * @param plugin the plugin to enable
     */
    public void enablePlugin(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        registerLoadedPlugin(plugin);
        if (plugin instanceof JavaPlugin javaPlugin) {
            javaPlugin.setEnabled(true);
        } else if (!plugin.isEnabled()) {
            plugin.onEnable();
        }
        delegateShim.registerPluginListener(
                plugin.getName(),
                "__plugin_enable_sentinel__",
                EventExecutionPhase.MONITOR,
                ignored -> {});
    }

    /**
     * Disables a plugin and invokes its {@link Plugin#onDisable()} lifecycle hook.
     *
     * @param plugin the plugin to disable
     */
    public void disablePlugin(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        if (plugin instanceof JavaPlugin javaPlugin) {
            javaPlugin.setEnabled(false);
        } else if (plugin.isEnabled()) {
            plugin.onDisable();
        }
    }

    /**
     * Reflectively scans {@code listener} for public single-argument event handler methods whose
     * parameter extends {@link Event} and binds them into the unified {@link PaperPluginManagerShim}.
     *
     * @param listener the event listener instance
     * @param plugin   the owning plugin
     */
    public void registerEvents(Listener listener, Plugin plugin) {
        Objects.requireNonNull(listener, "listener must not be null");
        Objects.requireNonNull(plugin, "plugin must not be null");
        registerLoadedPlugin(plugin);

        for (Method method : listener.getClass().getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers()) || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> paramType = method.getParameterTypes()[0];
            if (!Event.class.isAssignableFrom(paramType)) {
                continue;
            }
            method.setAccessible(true);
            String eventName = paramType.getSimpleName();
            delegateShim.registerPluginListener(
                    plugin.getName(),
                    eventName,
                    EventExecutionPhase.NORMAL,
                    eventObj -> {
                        if (paramType.isInstance(eventObj)) {
                            try {
                                method.invoke(listener, eventObj);
                            } catch (Exception e) {
                                throw new IllegalStateException(
                                        "Failed to invoke Bukkit event handler " + method, e);
                            }
                        }
                    });
        }
    }

    /**
     * Fires a Bukkit {@link Event} across all registered plugin listeners.
     *
     * @param event the event instance to dispatch
     */
    public void callEvent(Event event) {
        Objects.requireNonNull(event, "event must not be null");
        delegateShim.callBukkitEvent(event.getEventName(), event);
    }

    /**
     * Retrieves a registered plugin by name (case-insensitive).
     *
     * @param name the plugin name
     * @return the {@link Plugin} instance, or {@code null} if not found
     */
    public Plugin getPlugin(String name) {
        if (name == null) {
            return null;
        }
        return loadedPlugins.get(name.trim().toLowerCase());
    }

    /**
     * Returns an unmodifiable collection of all registered plugins.
     *
     * @return all registered plugins
     */
    public Collection<Plugin> getPlugins() {
        return Collections.unmodifiableCollection(loadedPlugins.values());
    }

    /**
     * Checks whether a plugin is enabled by name.
     *
     * @param name the plugin name
     * @return {@code true} if the plugin is loaded and enabled
     */
    public boolean isPluginEnabled(String name) {
        Plugin plugin = getPlugin(name);
        if (plugin != null) {
            return plugin.isEnabled();
        }
        return delegateShim.isPluginEnabled(name);
    }

    /**
     * Returns the underlying VESSEL {@link PaperPluginManagerShim}.
     *
     * @return the underlying shim
     */
    public PaperPluginManagerShim getDelegateShim() {
        return delegateShim;
    }
}
