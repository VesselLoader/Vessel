package net.minecraftforge.eventbus.api;

import dev.vessel.adapter.forge.SovereignEventBusShim;
import dev.vessel.kernel.event.EventExecutionPhase;
import dev.vessel.kernel.event.VesselEventSubscriberRegistry;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Canonical live implementation of Forge's {@link IEventBus} interface backed directly by VESSEL's
 * {@link SovereignEventBusShim} and high-throughput {@link VesselEventSubscriberRegistry}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public class VesselEventBus implements IEventBus {

    private final SovereignEventBusShim underlyingShim;
    private final VesselEventSubscriberRegistry subscriberRegistry;
    private final List<Object> registeredListenerInstances = new CopyOnWriteArrayList<>();

    /**
     * Constructs a new live Forge event bus backed by a fresh {@link SovereignEventBusShim}.
     */
    public VesselEventBus() {
        this(new SovereignEventBusShim());
    }

    /**
     * Constructs a live Forge event bus backed by the specified {@link SovereignEventBusShim}.
     *
     * @param underlyingShim non-null VESSEL event bus shim
     */
    public VesselEventBus(SovereignEventBusShim underlyingShim) {
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
    public boolean post(Object event) {
        Objects.requireNonNull(event, "event must not be null");
        boolean cancelled = underlyingShim.postEvent(event.getClass().getSimpleName(), event);
        try {
            underlyingShim.postEvent("*", event);
        } catch (ClassCastException ignored) {
            // Wildcard consumer with specific generic type did not match this event class
        }
        subscriberRegistry.dispatch(event, cancelled);
        return cancelled;
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
