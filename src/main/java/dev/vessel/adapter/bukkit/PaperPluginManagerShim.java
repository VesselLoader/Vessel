package dev.vessel.adapter.bukkit;

import dev.vessel.kernel.event.EventExecutionPhase;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Implements the Paper/Bukkit {@code PluginManager} event registration and dispatch contract
 * (Lessons L28 &amp; L45), executing listeners in deterministic {@link EventExecutionPhase} order.
 *
 * @version 1.0.0
 */
public final class PaperPluginManagerShim {

    private final Set<String> enabledPlugins = ConcurrentHashMap.newKeySet();
    private final Map<String, Map<EventExecutionPhase, List<Consumer<Object>>>> eventListeners =
            new ConcurrentHashMap<>();

    /**
     * Registers a Bukkit/Paper plugin event listener at the specified {@link EventExecutionPhase}
     * priority and marks the owning plugin as enabled.
     *
     * @param pluginName the owning plugin name
     * @param eventName  the canonical Bukkit event name
     * @param priority   the universal event execution phase priority
     * @param listener   the event consumer callback
     */
    public void registerPluginListener(
            String pluginName,
            String eventName,
            EventExecutionPhase priority,
            Consumer<Object> listener) {
        Objects.requireNonNull(pluginName, "pluginName must not be null");
        Objects.requireNonNull(eventName, "eventName must not be null");
        Objects.requireNonNull(priority, "priority must not be null");
        Objects.requireNonNull(listener, "listener must not be null");
        enabledPlugins.add(pluginName.trim().toLowerCase());
        Map<EventExecutionPhase, List<Consumer<Object>>> byPhase = eventListeners.computeIfAbsent(
                eventName,
                ignored -> new ConcurrentHashMap<>(new EnumMap<>(EventExecutionPhase.class)));
        byPhase.computeIfAbsent(priority, ignored -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * Dispatches a Bukkit event instance across all registered listeners in strict
     * {@link EventExecutionPhase} order from {@code EARLIEST} to {@code MONITOR}.
     *
     * @param eventName     the canonical Bukkit event name
     * @param eventInstance the event object payload
     * @return {@code true} if at least one listener was invoked
     */
    public boolean callBukkitEvent(String eventName, Object eventInstance) {
        Objects.requireNonNull(eventName, "eventName must not be null");
        Objects.requireNonNull(eventInstance, "eventInstance must not be null");
        Map<EventExecutionPhase, List<Consumer<Object>>> byPhase = eventListeners.get(eventName);
        if (byPhase == null || byPhase.isEmpty()) {
            return false;
        }
        boolean invokedAny = false;
        for (EventExecutionPhase phase : EventExecutionPhase.values()) {
            List<Consumer<Object>> listeners = byPhase.get(phase);
            if (listeners != null) {
                for (Consumer<Object> listener : listeners) {
                    listener.accept(eventInstance);
                    invokedAny = true;
                }
            }
        }
        return invokedAny;
    }

    /**
     * Checks whether a plugin is currently registered and enabled in the plugin manager.
     *
     * @param pluginName the plugin name
     * @return {@code true} if enabled
     */
    public boolean isPluginEnabled(String pluginName) {
        if (pluginName == null) {
            return false;
        }
        return enabledPlugins.contains(pluginName.trim().toLowerCase());
    }
}
