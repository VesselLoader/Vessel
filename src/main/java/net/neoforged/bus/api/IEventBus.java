package net.neoforged.bus.api;

import dev.vessel.adapter.forge.SovereignEventBusShim;
import java.util.List;
import java.util.function.Consumer;

/**
 * NeoForge event bus interface ({@code net.neoforged.bus.api.IEventBus}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.2
 */
public interface IEventBus {

    void register(Object target);

    void unregister(Object target);

    <T> void addListener(Consumer<T> consumer);

    <T> void addListener(Class<T> eventClass, Consumer<T> consumer);

    <T> void addListener(
            EventPriority priority,
            boolean receiveCanceled,
            Class<T> eventClass,
            Consumer<T> consumer);

    <T> T post(T event);

    List<Object> getRegisteredListeners();

    SovereignEventBusShim getUnderlyingShim();
}
