package dev.vessel.kernel.event;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * High-performance, array-baked subscriber registry that inspects event listener targets,
 * converts listener methods into direct {@link VesselEventSubscriberInvoker} handles, and
 * maintains zero-allocation pre-sorted dispatch arrays indexed by event class hierarchy.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class VesselEventSubscriberRegistry {

    private static final VesselEventSubscriberInvoker[] EMPTY_INVOKERS = new VesselEventSubscriberInvoker[0];

    private final List<VesselEventSubscriberInvoker> registeredInvokers = new CopyOnWriteArrayList<>();
    private final Map<Class<?>, VesselEventSubscriberInvoker[]> bakedDispatchCache = new ConcurrentHashMap<>();

    private static final Comparator<VesselEventSubscriberInvoker> PRIORITY_COMPARATOR =
            Comparator.comparingInt(VesselEventSubscriberInvoker::getPriorityOrdinal);

    /**
     * Constructs a new empty event subscriber registry.
     */
    public VesselEventSubscriberRegistry() {
    }

    /**
     * Registers all annotated event listener methods from the target instance or class.
     *
     * @param target target object instance or static class object
     */
    public void register(Object target) {
        Objects.requireNonNull(target, "target cannot be null");
        boolean isClassTarget = target instanceof Class<?>;
        Class<?> targetClass = isClassTarget ? (Class<?>) target : target.getClass();
        Object instance = isClassTarget ? null : target;

        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Method[] methods;
        try {
            methods = targetClass.getDeclaredMethods();
        } catch (Throwable t) {
            return;
        }

        List<VesselEventSubscriberInvoker> discovered = new ArrayList<>();
        for (Method method : methods) {
            if (method.getParameterCount() != 1) {
                continue;
            }
            boolean isStatic = Modifier.isStatic(method.getModifiers());
            if (isClassTarget && !isStatic) {
                continue;
            }

            int priorityOrdinal = 2; // Default NORMAL
            boolean receiveCancelled = false;
            boolean isSubscriber = false;

            for (Annotation annotation : method.getAnnotations()) {
                String annName = annotation.annotationType().getSimpleName();
                if ("SubscribeEvent".equals(annName)) {
                    isSubscriber = true;
                    try {
                        Method priorityMethod = annotation.annotationType().getMethod("priority");
                        Object prioVal = priorityMethod.invoke(annotation);
                        if (prioVal instanceof Enum<?> enumVal) {
                            priorityOrdinal = enumVal.ordinal();
                        }
                    } catch (Throwable ignored) {
                    }
                    try {
                        Method receiveCanceledMethod = annotation.annotationType().getMethod("receiveCanceled");
                        Object rcVal = receiveCanceledMethod.invoke(annotation);
                        if (rcVal instanceof Boolean boolVal) {
                            receiveCancelled = boolVal;
                        }
                    } catch (Throwable ignored) {
                    }
                    break;
                }
            }

            if (!isSubscriber) {
                continue;
            }

            try {
                method.setAccessible(true);
                Class<?> eventType = method.getParameterTypes()[0];
                VesselEventSubscriberInvoker invoker = new VesselEventSubscriberInvoker(
                        instance,
                        lookup.unreflect(method),
                        eventType,
                        priorityOrdinal,
                        receiveCancelled
                );
                discovered.add(invoker);
            } catch (Throwable t) {
                System.err.println("[VESSEL] Failed to bind event method handle for " + method + ": " + t.getMessage());
            }
        }

        if (!discovered.isEmpty()) {
            registeredInvokers.addAll(discovered);
            bakedDispatchCache.clear();
        }
    }

    /**
     * Unregisters all listener methods associated with the given target.
     *
     * @param target the target instance or class to remove
     */
    public void unregister(Object target) {
        if (target == null) {
            return;
        }
        boolean removed = registeredInvokers.removeIf(invoker -> invoker.matchesTarget(target));
        if (removed) {
            bakedDispatchCache.clear();
        }
    }

    /**
     * Resolves the pre-baked array of subscriber invokers matching the given event class,
     * including polymorphic superclasses and interfaces, sorted in ascending priority ordinal.
     *
     * @param eventClass the concrete event class being dispatched
     * @return non-null array of matching invokers
     */
    public VesselEventSubscriberInvoker[] getInvokersFor(Class<?> eventClass) {
        if (eventClass == null) {
            return EMPTY_INVOKERS;
        }
        return bakedDispatchCache.computeIfAbsent(eventClass, this::bakeInvokersForClass);
    }

    private VesselEventSubscriberInvoker[] bakeInvokersForClass(Class<?> eventClass) {
        List<VesselEventSubscriberInvoker> matches = new ArrayList<>();
        for (VesselEventSubscriberInvoker invoker : registeredInvokers) {
            if (invoker.getEventType().isAssignableFrom(eventClass)) {
                matches.add(invoker);
            }
        }
        if (matches.isEmpty()) {
            return EMPTY_INVOKERS;
        }
        VesselEventSubscriberInvoker[] array = matches.toArray(EMPTY_INVOKERS);
        Arrays.sort(array, PRIORITY_COMPARATOR);
        return array;
    }

    /**
     * Dispatches an event across all applicable invokers in priority order.
     *
     * @param event       the event payload
     * @param isCancelled whether the event has been marked as cancelled
     */
    public void dispatch(Object event, boolean isCancelled) {
        if (event == null) {
            return;
        }
        VesselEventSubscriberInvoker[] invokers = getInvokersFor(event.getClass());
        for (int i = 0; i < invokers.length; i++) {
            VesselEventSubscriberInvoker invoker = invokers[i];
            if (isCancelled && !invoker.isReceiveCancelled()) {
                continue;
            }
            invoker.invoke(event);
        }
    }

    /**
     * Clears all registered subscribers and invalidates cached dispatch arrays.
     */
    public void clear() {
        registeredInvokers.clear();
        bakedDispatchCache.clear();
    }

    /**
     * Returns the total count of registered subscriber methods.
     *
     * @return subscriber count
     */
    public int getSubscriberCount() {
        return registeredInvokers.size();
    }
}
