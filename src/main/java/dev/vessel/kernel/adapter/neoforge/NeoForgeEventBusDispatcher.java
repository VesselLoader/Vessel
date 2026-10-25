package dev.vessel.kernel.adapter.neoforge;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * High-throughput deterministic event bus conforming to NeoForge EventBus 8 specifications.
 * <p>
 * Invokes subscribers strictly according to {@link NeoForgeEventPriority} tiers (HIGHEST through LOWEST).
 * Respects {@link NeoForgeCancellableEvent} semantics and honors the {@code receiveCancelled} flag.
 * Eliminates ERR-96 (Concurrent listener mutation & priority inversion deadlock) and resolves ECO-031..040.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class NeoForgeEventBusDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(NeoForgeEventBusDispatcher.class);

    public static final String ERROR_CODE_MUTATION_DEADLOCK = "ERR-96";
    public static final String ERROR_CODE_ECOSYSTEM_EVENTBUS = "ECO-031";

    private final Map<Class<?>, Map<NeoForgeEventPriority, List<NeoForgePrioritizedSubscriber>>> bus =
            new ConcurrentHashMap<>();

    /**
     * Constructs a new NeoForge EventBus 8 dispatcher.
     */
    public NeoForgeEventBusDispatcher() {
    }

    /**
     * Subscribes a listener to a specific event type with priority and cancellation options.
     *
     * @param eventType        class token of the event to listen for
     * @param priority         execution priority tier
     * @param receiveCancelled whether this listener executes even when event is canceled
     * @param subscriber       execution callback
     * @param subscriberId     diagnostic identifier
     * @param <T>              event type parameter
     */
    public <T> void addListener(
            Class<T> eventType,
            NeoForgeEventPriority priority,
            boolean receiveCancelled,
            NeoForgeEventSubscriber subscriber,
            String subscriberId
    ) {
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(priority, "priority must not be null");
        Objects.requireNonNull(subscriber, "subscriber must not be null");
        Objects.requireNonNull(subscriberId, "subscriberId must not be null");

        Map<NeoForgeEventPriority, List<NeoForgePrioritizedSubscriber>> tierMap =
                bus.computeIfAbsent(eventType, k -> createPriorityMap());

        NeoForgePrioritizedSubscriber wrapper = new NeoForgePrioritizedSubscriber(
                priority,
                receiveCancelled,
                subscriber,
                subscriberId
        );

        tierMap.get(priority).add(wrapper);
        LOGGER.debug("[VESSEL-NEOFORGE-BUS] Registered subscriber '{}' for '{}' at priority {}",
                subscriberId, eventType.getSimpleName(), priority);
    }

    /**
     * Subscribes a listener with NORMAL priority and not receiving cancelled events.
     *
     * @param eventType    class token of the event
     * @param subscriber   callback
     * @param subscriberId diagnostic identifier
     * @param <T>          event type parameter
     */
    public <T> void addListener(Class<T> eventType, NeoForgeEventSubscriber subscriber, String subscriberId) {
        addListener(eventType, NeoForgeEventPriority.NORMAL, false, subscriber, subscriberId);
    }

    /**
     * Dispatches an event synchronously through all matching subscribers in deterministic priority order.
     *
     * @param event the event instance to dispatch
     * @return true if the event concluded uncanceled; false if canceled
     */
    public boolean post(Object event) {
        Objects.requireNonNull(event, "event must not be null");

        Class<?> eventClass = event.getClass();
        Map<NeoForgeEventPriority, List<NeoForgePrioritizedSubscriber>> tierMap = bus.get(eventClass);
        if (tierMap == null) {
            return isUncanceled(event);
        }

        boolean isCancellable = event instanceof NeoForgeCancellableEvent;
        NeoForgeCancellableEvent cancellable = isCancellable ? (NeoForgeCancellableEvent) event : null;

        // Execute in exact priority order: HIGHEST -> HIGH -> NORMAL -> LOW -> LOWEST
        for (NeoForgeEventPriority priority : NeoForgeEventPriority.values()) {
            List<NeoForgePrioritizedSubscriber> subscribers = tierMap.get(priority);
            if (subscribers == null || subscribers.isEmpty()) {
                continue;
            }

            for (NeoForgePrioritizedSubscriber sub : subscribers) {
                if (cancellable != null && cancellable.isCanceled() && !sub.receiveCancelled()) {
                    continue;
                }

                try {
                    sub.subscriber().onEvent(event);
                } catch (Exception ex) {
                    LOGGER.error("[VESSEL-NEOFORGE-BUS] [{}] Exception in subscriber '{}' for event '{}': {}",
                            ERROR_CODE_MUTATION_DEADLOCK, sub.subscriberId(), eventClass.getSimpleName(), ex.getMessage(), ex);
                    throw new NeoForgeEventBusException(
                            ERROR_CODE_MUTATION_DEADLOCK,
                            eventClass.getName(),
                            sub.subscriberId(),
                            ex.getMessage(),
                            ex
                    );
                }
            }
        }

        return isUncanceled(event);
    }

    /**
     * Unregisters all subscribers associated with a given subscriber ID across all event types.
     *
     * @param subscriberId identifier to remove
     * @return number of registrations removed
     */
    public int unregister(String subscriberId) {
        Objects.requireNonNull(subscriberId, "subscriberId must not be null");
        int count = 0;
        for (Map<NeoForgeEventPriority, List<NeoForgePrioritizedSubscriber>> tierMap : bus.values()) {
            for (List<NeoForgePrioritizedSubscriber> list : tierMap.values()) {
                List<NeoForgePrioritizedSubscriber> toRemove = new ArrayList<>();
                for (NeoForgePrioritizedSubscriber item : list) {
                    if (item.subscriberId().equals(subscriberId)) {
                        toRemove.add(item);
                    }
                }
                list.removeAll(toRemove);
                count += toRemove.size();
            }
        }
        return count;
    }

    /**
     * Clears all registered subscribers.
     */
    public void clear() {
        bus.clear();
    }

    private boolean isUncanceled(Object event) {
        if (event instanceof NeoForgeCancellableEvent c) {
            return !c.isCanceled();
        }
        return true;
    }

    private Map<NeoForgeEventPriority, List<NeoForgePrioritizedSubscriber>> createPriorityMap() {
        Map<NeoForgeEventPriority, List<NeoForgePrioritizedSubscriber>> map =
                new EnumMap<>(NeoForgeEventPriority.class);
        for (NeoForgeEventPriority p : NeoForgeEventPriority.values()) {
            map.put(p, new CopyOnWriteArrayList<>());
        }
        return map;
    }
}
