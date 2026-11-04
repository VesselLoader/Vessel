package net.minecraftforge.eventbus.api;

import dev.vessel.adapter.forge.SovereignEventBusShim;
import java.util.List;
import java.util.function.Consumer;

/**
 * Canonical Minecraft Forge event bus interface ({@code net.minecraftforge.eventbus.api.IEventBus}).
 *
 * <p>Supports {@link #register(Object)}, {@link #addListener(Consumer)}, and {@link #post(Object)}
 * for real compiled Forge mods across Minecraft eras.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.3
 */
public interface IEventBus {

    /**
     * Registers an event handler instance or class whose public 1-argument methods will be invoked
     * when matching events are posted.
     *
     * @param target listener object or {@link Class}
     */
    void register(Object target);

    /**
     * Unregisters an event handler instance or class.
     *
     * @param target listener object or {@link Class}
     */
    void unregister(Object target);

    /**
     * Registers a functional event consumer for the specified event channel.
     *
     * @param consumer callback consumer
     * @param <T>      event payload type
     */
    <T> void addListener(Consumer<T> consumer);

    /**
     * Registers a functional event consumer for a specific event class.
     *
     * @param eventClass event class token
     * @param consumer   callback consumer
     * @param <T>        event payload type
     */
    <T> void addListener(Class<T> eventClass, Consumer<T> consumer);

    /**
     * Registers a functional event consumer for a specific event class with explicit
     * {@link EventPriority} and cancellation semantics.
     *
     * @param priority        Forge execution priority
     * @param receiveCanceled whether to receive cancelled events
     * @param eventClass      event class token
     * @param consumer        callback consumer
     * @param <T>             event payload type
     */
    <T> void addListener(
            EventPriority priority,
            boolean receiveCanceled,
            Class<T> eventClass,
            Consumer<T> consumer);

    /**
     * Dispatches an event across all functional listeners and registered object handlers.
     *
     * @param event non-null event payload
     * @return {@code true} if the event was cancelled, {@code false} otherwise
     */
    boolean post(Object event);

    /**
     * Returns an unmodifiable view of all object/class listeners registered via {@link #register(Object)}.
     *
     * @return unmodifiable list of registered listener targets
     */
    List<Object> getRegisteredListeners();

    /**
     * Returns VESSEL's underlying {@link SovereignEventBusShim}.
     *
     * @return underlying event bus shim
     */
    SovereignEventBusShim getUnderlyingShim();
}
