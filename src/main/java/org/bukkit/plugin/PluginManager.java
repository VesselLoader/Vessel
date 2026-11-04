package org.bukkit.plugin;

import dev.vessel.adapter.bukkit.PaperPluginManagerShim;
import java.util.Collection;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;

/**
 * Standard Bukkit/Paper {@code PluginManager} interface contract defining plugin lifecycle
 * activation, reflective event registration, and event dispatch.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.2
 */
public interface PluginManager {

    /**
     * Registers a loaded {@link Plugin} instance with the plugin manager.
     *
     * @param plugin the plugin instance
     */
    void registerLoadedPlugin(Plugin plugin);

    /**
     * Enables a plugin and invokes its {@link Plugin#onEnable()} lifecycle hook.
     *
     * @param plugin the plugin to enable
     */
    void enablePlugin(Plugin plugin);

    /**
     * Disables a plugin and invokes its {@link Plugin#onDisable()} lifecycle hook.
     *
     * @param plugin the plugin to disable
     */
    void disablePlugin(Plugin plugin);

    /**
     * Scans {@code listener} for public single-argument event handler methods whose
     * parameter extends {@link Event} and binds them into the event dispatch pipeline.
     *
     * @param listener the event listener instance
     * @param plugin   the owning plugin
     */
    void registerEvents(Listener listener, Plugin plugin);

    /**
     * Fires a Bukkit {@link Event} across all registered plugin listeners.
     *
     * @param event the event instance to dispatch
     */
    void callEvent(Event event);

    /**
     * Retrieves a registered plugin by name (case-insensitive).
     *
     * @param name the plugin name
     * @return the {@link Plugin} instance, or {@code null} if not found
     */
    Plugin getPlugin(String name);

    /**
     * Returns an unmodifiable collection of all registered plugins.
     *
     * @return all registered plugins
     */
    Collection<Plugin> getPlugins();

    /**
     * Checks whether a plugin is enabled by name.
     *
     * @param name the plugin name
     * @return {@code true} if the plugin is loaded and enabled
     */
    boolean isPluginEnabled(String name);

    /**
     * Returns the underlying VESSEL {@link PaperPluginManagerShim}.
     *
     * @return the underlying shim
     */
    PaperPluginManagerShim getDelegateShim();
}
