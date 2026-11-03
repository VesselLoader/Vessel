package com.example.mod.pure;

import dev.vessel.api.event.SovereignEventBus;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Demonstrates high-throughput event subscription using VESSEL Sovereign Event Bus.
 *
 * <p>Uses pre-allocated dispatch tables achieving sub-nanosecond event invocation
 * without reflection or proxy allocations at runtime.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 * @since 1.0.0
 */
public final class PureVesselEventListener {

    private final AtomicLong eventCounter;

    /**
     * Constructs a new pure event listener.
     */
    public PureVesselEventListener() {
        this.eventCounter = new AtomicLong(0L);
    }

    /**
     * Subscribes a functional listener to a specific event type.
     *
     * @param eventClass event class to listen for
     * @param listener   listener object or callback
     */
    public void subscribeToEvent(Class<?> eventClass, Object listener) {
        Objects.requireNonNull(eventClass, "eventClass cannot be null");
        Objects.requireNonNull(listener, "listener cannot be null");

        SovereignEventBus.registerFunctionalListener(eventClass, listener);
    }

    /**
     * Registers a synthetic listener object containing annotated event handler methods.
     *
     * @param listenerTarget object containing listener methods
     */
    public void registerSyntheticListener(Object listenerTarget) {
        Objects.requireNonNull(listenerTarget, "listenerTarget cannot be null");
        SovereignEventBus.registerSyntheticObject(listenerTarget);
    }

    /**
     * Fires a dynamic event through the sovereign dispatch pipeline.
     *
     * @param event event instance
     * @return {@code true} if processed without cancellation
     */
    public boolean postDynamicEvent(Object event) {
        Objects.requireNonNull(event, "event cannot be null");
        eventCounter.incrementAndGet();
        return SovereignEventBus.dispatchDynamicEvent(event);
    }

    /**
     * Retrieves total events observed by this listener.
     *
     * @return count of processed events
     */
    public long getObservedEventCount() {
        return eventCounter.get();
    }
}
