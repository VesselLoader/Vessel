package net.neoforged.bus.api;

import dev.vessel.adapter.forge.SovereignEventBusShim;
import dev.vessel.kernel.event.EventExecutionPhase;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Canonical NeoForge event bus runtime surface ({@code net.neoforged.bus.api.IEventBus})
 * bridged directly to VESSEL's {@link SovereignEventBusShim}.
 *
 * <p>Supports NeoForge constructor injection ({@code public MyMod(IEventBus modEventBus)}) as well
 * as {@link #register(Object)}, {@link #addListener(Consumer)}, and {@link #post(Object)}.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public class IEventBus {

    private final SovereignEventBusShim underlyingShim;
    private final List<Object> registeredListenerInstances = new CopyOnWriteArrayList<>();

    /**
     * Constructs a new live NeoForge {@code IEventBus} backed by a fresh {@link SovereignEventBusShim}.
     */
    public IEventBus() {
        this(new SovereignEventBusShim());
    }

    /**
     * Constructs a live NeoForge {@code IEventBus} backed by the specified {@link SovereignEventBusShim}.
     *
     * @param underlyingShim non-null VESSEL event bus shim
     */
    public IEventBus(SovereignEventBusShim underlyingShim) {
        this.underlyingShim = Objects.requireNonNull(underlyingShim, "underlyingShim must not be null");
    }

    /**
     * Registers an event handler instance or class whose 1-argument methods will be invoked when
     * matching events are posted.
     *
     * @param target listener object or {@link Class}
     */
    public void register(Object target) {
        Objects.requireNonNull(target, "target must not be null");
        registeredListenerInstances.add(target);
    }

    /**
     * Registers a functional event consumer on this NeoForge event bus.
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
     * Registers a functional event consumer with an explicit {@link EventPriority}, cancellation filter,
     * and event class token.
     *
     * @param priority        NeoForge execution priority
     * @param receiveCanceled whether to invoke the listener even if the event was cancelled
     * @param eventClass      event class token
     * @param consumer        callback consumer
     * @param <T>             event payload type
     */
    @SuppressWarnings("unchecked")
    public <T> void addListener(
            EventPriority priority,
            boolean receiveCanceled,
            Class<T> eventClass,
            Consumer<T> consumer) {
        Objects.requireNonNull(priority, "priority must not be null");
        Objects.requireNonNull(eventClass, "eventClass must not be null");
        Objects.requireNonNull(consumer, "consumer must not be null");
        EventExecutionPhase executionPhase = EventExecutionPhase.fromForge(priority.name());
        underlyingShim.registerListener(
                eventClass.getSimpleName(),
                executionPhase,
                receiveCanceled,
                (Consumer<Object>) consumer);
    }

    /**
     * Posts an event across all functional listeners and registered object handlers.
     *
     * @param event non-null event payload
     * @param <T>   event payload type
     * @return the posted event payload
     */
    public <T> T post(T event) {
        Objects.requireNonNull(event, "event must not be null");
        underlyingShim.postEvent(event.getClass().getSimpleName(), event);
        try {
            underlyingShim.postEvent("*", event);
        } catch (ClassCastException ignored) {
            // Wildcard consumer registered via addListener(Consumer<T>) had a non-matching generic type
        }
        dispatchToRegisteredObjects(event);
        return event;
    }

    /**
     * Returns an unmodifiable view of all listener instances and classes registered via {@link #register(Object)}.
     *
     * @return unmodifiable list of registered listener objects
     */
    public List<Object> getRegisteredListeners() {
        return Collections.unmodifiableList(registeredListenerInstances);
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
        for (EventPriority currentPriority : EventPriority.values()) {
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
                    SubscribeEvent subscribeAnnotation = method.getAnnotation(SubscribeEvent.class);
                    EventPriority effectivePriority = subscribeAnnotation != null
                            ? subscribeAnnotation.priority()
                            : EventPriority.NORMAL;
                    if (effectivePriority != currentPriority) {
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
}
