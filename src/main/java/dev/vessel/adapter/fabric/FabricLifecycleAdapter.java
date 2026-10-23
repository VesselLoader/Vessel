package dev.vessel.adapter.fabric;

import dev.vessel.kernel.event.EventExecutionPhase;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Coordinates Fabric and Quilt entrypoint invocation order ({@code preLaunch},
 * {@code main}, {@code server} per Lesson L22) and bridges Fabric {@code Event<T>}
 * callbacks into the VESSEL kernel's {@link EventExecutionPhase} pipeline.
 *
 * @version 1.0.0
 */
public final class FabricLifecycleAdapter {

    private final Map<String, List<Runnable>> phaseEntrypoints = new ConcurrentHashMap<>();
    private final Map<String, Map<EventExecutionPhase, List<Consumer<Object>>>> bridgedEvents =
            new ConcurrentHashMap<>();

    /**
     * Creates a new Fabric lifecycle adapter.
     */
    public FabricLifecycleAdapter() {
    }

    /**
     * Registers a mod entrypoint callback for a lifecycle phase ({@code preLaunch},
     * {@code main}, or {@code server}).
     *
     * @param phaseKey    the entrypoint phase key
     * @param modId       the owning mod ID
     * @param initializer the entrypoint runnable
     */
    public void registerEntrypoint(String phaseKey, String modId, Runnable initializer) {
        Objects.requireNonNull(phaseKey, "phaseKey must not be null");
        Objects.requireNonNull(modId, "modId must not be null");
        Objects.requireNonNull(initializer, "initializer must not be null");
        List<Runnable> callbacks =
                phaseEntrypoints.computeIfAbsent(phaseKey.trim(), k -> new CopyOnWriteArrayList<>());
        callbacks.add(initializer);
    }

    /**
     * Sequentially invokes all registered entrypoints for the specified phase.
     *
     * @param phaseKey the entrypoint phase key (e.g., {@code "preLaunch"}, {@code "main"}, {@code "server"})
     * @return the number of entrypoints invoked
     */
    public int invokeEntrypointsForPhase(String phaseKey) {
        Objects.requireNonNull(phaseKey, "phaseKey must not be null");
        List<Runnable> callbacks = phaseEntrypoints.get(phaseKey.trim());
        if (callbacks == null || callbacks.isEmpty()) {
            return 0;
        }
        int invoked = 0;
        for (Runnable callback : callbacks) {
            callback.run();
            invoked++;
        }
        return invoked;
    }

    /**
     * Bridges a Fabric {@code Event<T>} listener into the kernel's phase-ordered event pipeline.
     *
     * @param eventName canonical Fabric event identifier
     * @param phase     target kernel execution phase
     * @param listener  event consumer callback
     */
    public void bridgeFabricEventToKernel(
            String eventName, EventExecutionPhase phase, Consumer<Object> listener) {
        Objects.requireNonNull(eventName, "eventName must not be null");
        Objects.requireNonNull(phase, "phase must not be null");
        Objects.requireNonNull(listener, "listener must not be null");
        Map<EventExecutionPhase, List<Consumer<Object>>> phaseMap =
                bridgedEvents.computeIfAbsent(
                        eventName.trim(),
                        k -> new EnumMap<>(EventExecutionPhase.class));
        synchronized (phaseMap) {
            List<Consumer<Object>> list =
                    phaseMap.computeIfAbsent(phase, p -> new CopyOnWriteArrayList<>());
            list.add(listener);
        }
    }

    /**
     * Dispatches a bridged Fabric event across all registered {@link EventExecutionPhase} tiers
     * in deterministic phase order.
     *
     * @param eventName canonical Fabric event identifier
     * @param payload   event payload object
     * @return total number of listeners invoked
     */
    public int dispatchBridgedEvent(String eventName, Object payload) {
        Objects.requireNonNull(eventName, "eventName must not be null");
        Map<EventExecutionPhase, List<Consumer<Object>>> phaseMap = bridgedEvents.get(eventName.trim());
        if (phaseMap == null) {
            return 0;
        }
        int count = 0;
        for (EventExecutionPhase phase : EventExecutionPhase.values()) {
            List<Consumer<Object>> listeners;
            synchronized (phaseMap) {
                listeners = phaseMap.get(phase);
            }
            if (listeners != null) {
                for (Consumer<Object> listener : listeners) {
                    listener.accept(payload);
                    count++;
                }
            }
        }
        return count;
    }
}
