package dev.vessel.kernel.event;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Universal multi-loader event bus coordinating phase-ordered listener execution.
 * <p>
 * Manages event dispatch across all 6 sequential {@link EventExecutionPhase} tiers.
 * For each event class, listeners are segregated by phase and baked into high-performance
 * {@link ArrayBackedEventInvoker} arrays, ensuring deterministic ordering between Bukkit,
 * Forge, NeoForge, and Fabric listeners.
 *
 * @version 1.0.1
 */
public final class UniversalEventDispatcher {

    private final Map<Class<?>, PhaseBuses<?>> eventBuses;

    /**
     * Constructs a new universal event dispatcher.
     */
    public UniversalEventDispatcher() {
        this.eventBuses = new ConcurrentHashMap<>();
    }

    /**
     * Registers an event listener at the specified execution priority phase.
     *
     * @param eventClass event class token
     * @param phase      execution phase (EARLIEST to MONITOR)
     * @param listener   listener delegate
     * @param <T>        event type
     */
    @SuppressWarnings("unchecked")
    public <T> void register(Class<T> eventClass, EventExecutionPhase phase, Consumer<T> listener) {
        Objects.requireNonNull(eventClass, "eventClass cannot be null");
        Objects.requireNonNull(phase, "phase cannot be null");
        Objects.requireNonNull(listener, "listener cannot be null");

        PhaseBuses<T> buses = (PhaseBuses<T>) eventBuses.computeIfAbsent(eventClass, k -> new PhaseBuses<>());
        buses.getInvoker(phase).register(listener);
    }

    /**
     * Dispatches an event through all 6 execution phases sequentially.
     *
     * @param event the event to dispatch
     * @param <T>   event type
     * @return the dispatched event
     */
    @SuppressWarnings("unchecked")
    public <T> T dispatch(T event) {
        Objects.requireNonNull(event, "event cannot be null");

        PhaseBuses<T> buses = (PhaseBuses<T>) eventBuses.get(event.getClass());
        if (buses == null) {
            return event; // fast path: zero listeners registered
        }

        boolean isCancellable = event instanceof VesselCancellable;

        for (EventExecutionPhase phase : EventExecutionPhase.values()) {
            ArrayBackedEventInvoker<T> invoker = buses.getInvoker(phase);
            if (invoker.getListenerCount() == 0) {
                continue;
            }

            // If cancelled, skip non-monitor phases
            if (isCancellable && ((VesselCancellable) event).isCancelled() && phase != EventExecutionPhase.MONITOR) {
                continue;
            }

            // Monitor phase executes even when cancelled
            boolean checkCancellation = (phase != EventExecutionPhase.MONITOR);
            invoker.dispatch(event, checkCancellation);
        }

        return event;
    }

    /**
     * Returns the total number of listeners registered for the given event class across all phases.
     *
     * @param eventClass event class token
     * @return listener count
     */
    public int getListenerCount(Class<?> eventClass) {
        PhaseBuses<?> buses = eventBuses.get(eventClass);
        if (buses == null) {
            return 0;
        }
        return buses.getTotalListenerCount();
    }

    /**
     * Clears all registered event listeners.
     */
    public void clearAll() {
        eventBuses.clear();
    }

    private static final class PhaseBuses<T> {
        private final ArrayBackedEventInvoker<T>[] invokers;

        @SuppressWarnings("unchecked")
        PhaseBuses() {
            int len = EventExecutionPhase.values().length;
            this.invokers = (ArrayBackedEventInvoker<T>[]) new ArrayBackedEventInvoker<?>[len];
            for (int i = 0; i < len; i++) {
                invokers[i] = new ArrayBackedEventInvoker<>();
            }
        }

        ArrayBackedEventInvoker<T> getInvoker(EventExecutionPhase phase) {
            return invokers[phase.getOrder()];
        }

        int getTotalListenerCount() {
            int total = 0;
            for (ArrayBackedEventInvoker<T> invoker : invokers) {
                total += invoker.getListenerCount();
            }
            return total;
        }
    }
}
