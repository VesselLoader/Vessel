package net.neoforged.bus.api;

import dev.vessel.adapter.forge.SovereignEventBusShim;
import dev.vessel.kernel.event.EventExecutionPhase;
import dev.vessel.kernel.event.VesselEventSubscriberRegistry;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Live NeoForge event bus implementation backed directly by VESSEL's {@link SovereignEventBusShim}
 * and high-throughput {@link VesselEventSubscriberRegistry}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.2
 * @author VESSEL Engine Team
 */
public class VesselNeoEventBus implements IEventBus {

    private final SovereignEventBusShim underlyingShim;
    private final VesselEventSubscriberRegistry subscriberRegistry;
    private final List<Object> registeredListenerInstances = new CopyOnWriteArrayList<>();

    /**
     * Constructs a new live NeoForge event bus backed by a fresh {@link SovereignEventBusShim}.
     */
    public VesselNeoEventBus() {
        this(new SovereignEventBusShim());
    }

    /**
     * Constructs a live NeoForge event bus backed by the specified {@link SovereignEventBusShim}.
     *
     * @param underlyingShim non-null VESSEL event bus shim
     */
    public VesselNeoEventBus(SovereignEventBusShim underlyingShim) {
        this.underlyingShim = Objects.requireNonNull(underlyingShim, "underlyingShim must not be null");
        this.subscriberRegistry = new VesselEventSubscriberRegistry();
    }

    @Override
    public void register(Object target) {
        Objects.requireNonNull(target, "target must not be null");
        registeredListenerInstances.add(target);
        subscriberRegistry.register(target);
    }

    @Override
    public void unregister(Object target) {
        if (target != null) {
            registeredListenerInstances.remove(target);
            subscriberRegistry.unregister(target);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void addListener(Consumer<T> consumer) {
        Objects.requireNonNull(consumer, "consumer must not be null");
        underlyingShim.registerListener(
                "*",
                EventExecutionPhase.NORMAL,
                false,
                (Consumer<Object>) consumer);
    }

    @Override
    public <T> void addListener(Class<T> eventClass, Consumer<T> consumer) {
        addListener(EventPriority.NORMAL, false, eventClass, consumer);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void addListener(
            EventPriority priority,
            boolean receiveCanceled,
            Class<T> eventClass,
            Consumer<T> consumer) {
        Objects.requireNonNull(eventClass, "eventClass must not be null");
        Objects.requireNonNull(consumer, "consumer must not be null");
        EventExecutionPhase phase = EventExecutionPhase.fromForge(priority != null ? priority.name() : null);
        underlyingShim.registerListener(
                eventClass.getSimpleName(),
                phase,
                receiveCanceled,
                (Consumer<Object>) consumer);
    }

    @Override
    public <T> T post(T event) {
        Objects.requireNonNull(event, "event must not be null");
        boolean cancelled = underlyingShim.postEvent(event.getClass().getSimpleName(), event);
        try {
            underlyingShim.postEvent("*", event);
        } catch (ClassCastException ignored) {
            // Wildcard consumer with specific generic type did not match this event class
        }
        subscriberRegistry.dispatch(event, cancelled);
        return event;
    }

    @Override
    public List<Object> getRegisteredListeners() {
        return Collections.unmodifiableList(registeredListenerInstances);
    }

    @Override
    public SovereignEventBusShim getUnderlyingShim() {
        return underlyingShim;
    }

    /**
     * Exposes the active subscriber registry for telemetry and verification.
     *
     * @return subscriber registry
     */
    public VesselEventSubscriberRegistry getSubscriberRegistry() {
        return subscriberRegistry;
    }
}
