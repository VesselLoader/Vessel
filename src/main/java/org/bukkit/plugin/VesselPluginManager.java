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
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Concrete implementation of Bukkit/Paper {@link PluginManager} backed directly by VESSEL's
 * {@link PaperPluginManagerShim}. Supports plugin lifecycle activation, reflective event handler
 * discovery on {@link Listener} instances, and synchronous/asynchronous event dispatch.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public class VesselPluginManager implements PluginManager {

    private final PaperPluginManagerShim delegateShim = new PaperPluginManagerShim();
    private final Map<String, Plugin> loadedPlugins = new ConcurrentHashMap<>();

    public VesselPluginManager() {
    }

    @Override
    public void registerLoadedPlugin(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        loadedPlugins.put(plugin.getName().toLowerCase(), plugin);
    }

    @Override
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

    @Override
    public void disablePlugin(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        if (plugin instanceof JavaPlugin javaPlugin) {
            javaPlugin.setEnabled(false);
        } else if (plugin.isEnabled()) {
            plugin.onDisable();
        }
    }

    @Override
    public void registerEvents(Listener listener, Plugin plugin) {
        Objects.requireNonNull(listener, "listener must not be null");
        Objects.requireNonNull(plugin, "plugin must not be null");
        registerLoadedPlugin(plugin);

        Method[] methods;
        try {
            methods = listener.getClass().getDeclaredMethods();
        } catch (Throwable t) {
            return;
        }

        for (Method method : methods) {
            try {
                if (!Modifier.isPublic(method.getModifiers()) || method.getParameterCount() != 1) {
                    continue;
                }
                Class<?> paramType = method.getParameterTypes()[0];
                if (!Event.class.isAssignableFrom(paramType)) {
                    continue;
                }
                EventHandler annotation = method.getAnnotation(EventHandler.class);
                EventPriority bukkitPriority =
                        (annotation != null) ? annotation.priority() : EventPriority.NORMAL;
                boolean ignoreCancelled = (annotation != null) && annotation.ignoreCancelled();
                EventExecutionPhase executionPhase =
                        EventExecutionPhase.fromBukkit(bukkitPriority.name());
                String eventName = paramType.getSimpleName();
                ReflectiveBukkitEventConsumer consumer = new ReflectiveBukkitEventConsumer(
                        listener,
                        method,
                        paramType,
                        ignoreCancelled,
                        bukkitPriority == EventPriority.MONITOR);
                delegateShim.registerPluginListener(
                        plugin.getName(),
                        eventName,
                        executionPhase,
                        consumer);
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void callEvent(Event event) {
        Objects.requireNonNull(event, "event must not be null");
        delegateShim.callBukkitEvent(event.getEventName(), event);
    }

    @Override
    public Plugin getPlugin(String name) {
        if (name == null) {
            return null;
        }
        return loadedPlugins.get(name.trim().toLowerCase());
    }

    @Override
    public Collection<Plugin> getPlugins() {
        return Collections.unmodifiableCollection(loadedPlugins.values());
    }

    @Override
    public boolean isPluginEnabled(String name) {
        Plugin plugin = getPlugin(name);
        if (plugin != null) {
            return plugin.isEnabled();
        }
        return delegateShim.isPluginEnabled(name);
    }

    @Override
    public PaperPluginManagerShim getDelegateShim() {
        return delegateShim;
    }
}
