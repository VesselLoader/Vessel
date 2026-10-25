package net.minecraftforge.eventbus.api;

import dev.vessel.adapter.forge.SovereignEventBusShim;
import dev.vessel.kernel.event.EventExecutionPhase;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Canonical Minecraft Forge event bus runtime surface
 * ({@code net.minecraftforge.eventbus.api.IEventBus}) bridged directly to VESSEL's
 * {@link SovereignEventBusShim}.
 *
 * <p>Supports {@link #register(Object)}, {@link #addListener(Consumer)}, and {@link #post(Object)}
 * for real compiled Forge mods across Minecraft eras.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class IEventBus {

    private final SovereignEventBusShim underlyingShim;
    private final List<Object> registeredListenerInstances = new CopyOnWriteArrayList<>();

    /**
     * Constructs a new live Forge {@code IEventBus} backed by a fresh {@link SovereignEventBusShim}.
     */
    public IEventBus() {
        this(new SovereignEventBusShim());
    }

    /**
     * Constructs a live Forge {@code IEventBus} backed by the specified {@link SovereignEventBusShim}.
     *
     * @param underlyingShim non-null VESSEL event bus shim
     */
    public IEventBus(SovereignEventBusShim underlyingShim) {
        this.underlyingShim = Objects.requireNonNull(underlyingShim, "underlyingShim must not be null");
    }

    /**
     * Registers an event handler instance or class whose public 1-argument methods will be invoked
     * when matching events are posted.
     *
     * @param target listener object or {@link Class}
     */
    public void register(Object target) {
        Objects.requireNonNull(target, "target must not be null");
        registeredListenerInstances.add(target);
    }

    /**
     * Registers a functional event consumer for the specified event channel.
     *
     * @param consumer callback consumer
     * @param <T>      event payload type
     */
    @SuppressWarnings("unchecked")
    public <T> void addListener(Consumer<T> consumer) {
        Objects.requireNonNull(consumer, "consumer must not be null");
        underlyingShim.registerListener(
                "*",
                EventExecutionPhase.NORMAL,
                false,
                (Consumer<Object>) consumer);
    }

    /**
     * Registers a functional event consumer for a specific event class.
     *
     * @param eventClass event class token
     * @param consumer   callback consumer
     * @param <T>        event payload type
     */
    @SuppressWarnings("unchecked")
    public <T> void addListener(Class<T> eventClass, Consumer<T> consumer) {
        Objects.requireNonNull(eventClass, "eventClass must not be null");
        Objects.requireNonNull(consumer, "consumer must not be null");
        underlyingShim.registerListener(
                eventClass.getSimpleName(),
                EventExecutionPhase.NORMAL,
                false,
                (Consumer<Object>) consumer);
    }

    /**
     * Dispatches an event across all functional listeners and registered object handlers.
     *
     * @param event non-null event payload
     * @return {@code true} if the event was cancelled, {@code false} otherwise
     */
    public boolean post(Object event) {
        Objects.requireNonNull(event, "event must not be null");
        boolean cancelled = underlyingShim.postEvent(event.getClass().getSimpleName(), event);
        underlyingShim.postEvent("*", event);
        dispatchToRegisteredObjects(event);
        return cancelled;
    }

    /**
     * Returns VESSEL's underlying {@link SovereignEventBusShim}.
     *
     * @return underlying event bus shim
     */
    public SovereignEventBusShim getUnderlyingShim() {
        return underlyingShim;
    }

    private void dispatchToRegisteredObjects(Object event) {
        for (Object target : registeredListenerInstances) {
            boolean isClassTarget = target instanceof Class<?>;
            Class<?> targetClass = isClassTarget ? (Class<?>) target : target.getClass();
            Object invokeTarget = isClassTarget ? null : target;
            Method[] methods = targetClass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getParameterCount() != 1) {
                    continue;
                }
                if (isClassTarget && !Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                Class<?> paramType = method.getParameterTypes()[0];
                if (paramType.isAssignableFrom(event.getClass())) {
                    try {
                        method.setAccessible(true);
                        method.invoke(invokeTarget, event);
                    } catch (ReflectiveOperationException ignored) {
                        // Guard against faulty third-party listener methods
                    }
                }
            }
        }
    }
}
