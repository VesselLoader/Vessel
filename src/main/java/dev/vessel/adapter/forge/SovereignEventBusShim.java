package dev.vessel.adapter.forge;

import dev.vessel.kernel.event.EventExecutionPhase;
import dev.vessel.kernel.event.VesselCancellable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Clean-room ABI shim for Forge and NeoForge {@code IEventBus}, dispatching events
 * across {@link EventExecutionPhase} priority tiers while honoring cancellation
 * and {@code receiveCancelled} subscription semantics.
 *
 * @version 1.0.0
 */
public final class SovereignEventBusShim {

    private final Map<String, Map<EventExecutionPhase, List<Consumer<Object>>>> phaseListeners =
            new ConcurrentHashMap<>();
    private final Set<Consumer<Object>> receiveCancelledHandlers =
            ConcurrentHashMap.newKeySet();

    /**
     * Creates a new Forge/NeoForge event bus shim.
     */
    public SovereignEventBusShim() {
    }

    /**
     * Registers an event listener for the specified event type and priority phase.
     *
     * @param eventType        canonical event type name
     * @param priority         execution priority phase
     * @param receiveCancelled whether this listener should still be invoked after event cancellation
     * @param handler          listener callback
     */
    public void registerListener(
            String eventType,
            EventExecutionPhase priority,
            boolean receiveCancelled,
            Consumer<Object> handler) {
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(priority, "priority must not be null");
        Objects.requireNonNull(handler, "handler must not be null");

        if (receiveCancelled) {
            receiveCancelledHandlers.add(handler);
        }
        Map<EventExecutionPhase, List<Consumer<Object>>> byPhase =
                phaseListeners.computeIfAbsent(
                        eventType.trim(),
                        k -> new EnumMap<>(EventExecutionPhase.class));
        synchronized (byPhase) {
            List<Consumer<Object>> list =
                    byPhase.computeIfAbsent(priority, p -> new CopyOnWriteArrayList<>());
            list.add(handler);
        }
    }

    /**
     * Posts an event payload across all registered listeners in phase order.
     *
     * @param eventType    canonical event type name
     * @param eventPayload the event object (may implement {@link VesselCancellable})
     * @return {@code true} if the event was cancelled during dispatch, {@code false} otherwise
     */
    public boolean postEvent(String eventType, Object eventPayload) {
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(eventPayload, "eventPayload must not be null");

        Map<EventExecutionPhase, List<Consumer<Object>>> byPhase =
                phaseListeners.get(eventType.trim());
        if (byPhase == null) {
            return isEventCancelled(eventPayload);
        }

        for (EventExecutionPhase phase : EventExecutionPhase.values()) {
            List<Consumer<Object>> handlers;
            synchronized (byPhase) {
                handlers = byPhase.get(phase);
            }
            if (handlers == null || handlers.isEmpty()) {
                continue;
            }
            for (Consumer<Object> handler : handlers) {
                if (isEventCancelled(eventPayload)
                        && phase != EventExecutionPhase.MONITOR
                        && !receiveCancelledHandlers.contains(handler)) {
                    continue;
                }
                handler.accept(eventPayload);
            }
        }
        return isEventCancelled(eventPayload);
    }

    private static boolean isEventCancelled(Object eventPayload) {
        if (eventPayload instanceof VesselCancellable cancellable) {
            return cancellable.isCancelled();
        }
        if (eventPayload instanceof java.util.concurrent.atomic.AtomicBoolean flag) {
            return flag.get();
        }
        return false;
    }
}
