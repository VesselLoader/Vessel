package dev.vessel.kernel.event;

import java.lang.invoke.MethodHandle;
import java.util.Objects;

/**
 * High-performance, zero-allocation invoker wrapper that dispatches events directly to a
 * target listener via pre-resolved {@link MethodHandle}, eliminating dynamic reflection
 * overhead during runtime event execution loops.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class VesselEventSubscriberInvoker {

    private final Object targetInstance;
    private final MethodHandle methodHandle;
    private final Class<?> eventType;
    private final int priorityOrdinal;
    private final boolean receiveCancelled;
    private final boolean isStatic;

    /**
     * Constructs a new subscriber invoker.
     *
     * @param targetInstance   the listener instance, or {@code null} if the method is static
     * @param methodHandle     the unreflected direct method handle
     * @param eventType        the exact event class accepted by this subscriber
     * @param priorityOrdinal  the priority rank for deterministic ordering
     * @param receiveCancelled whether this subscriber should receive already-cancelled events
     */
    public VesselEventSubscriberInvoker(
            Object targetInstance,
            MethodHandle methodHandle,
            Class<?> eventType,
            int priorityOrdinal,
            boolean receiveCancelled) {
        this.targetInstance = targetInstance;
        this.methodHandle = Objects.requireNonNull(methodHandle, "methodHandle cannot be null");
        this.eventType = Objects.requireNonNull(eventType, "eventType cannot be null");
        this.priorityOrdinal = priorityOrdinal;
        this.receiveCancelled = receiveCancelled;
        this.isStatic = (targetInstance == null);
    }

    /**
     * Invokes the underlying subscriber method with the given event payload.
     *
     * @param event the event payload
     */
    public void invoke(Object event) {
        if (event == null || !eventType.isInstance(event)) {
            return;
        }
        try {
            if (isStatic) {
                methodHandle.invoke(event);
            } else {
                methodHandle.invoke(targetInstance, event);
            }
        } catch (Throwable throwable) {
            System.err.println("[VESSEL] Exception dispatching event " + eventType.getName()
                    + " to subscriber: " + throwable.getMessage());
        }
    }

    /**
     * Checks if this subscriber belongs to the specified target instance or class.
     *
     * @param target the target listener object or class
     * @return true if this subscriber is bound to the target
     */
    public boolean matchesTarget(Object target) {
        if (target == null) {
            return false;
        }
        if (target instanceof Class<?> targetClass) {
            return isStatic && targetClass.equals(eventType.getDeclaringClass());
        }
        return Objects.equals(this.targetInstance, target);
    }

    public Object getTargetInstance() {
        return targetInstance;
    }

    public MethodHandle getMethodHandle() {
        return methodHandle;
    }

    public Class<?> getEventType() {
        return eventType;
    }

    public int getPriorityOrdinal() {
        return priorityOrdinal;
    }

    public boolean isReceiveCancelled() {
        return receiveCancelled;
    }

    public boolean isStatic() {
        return isStatic;
    }
}
