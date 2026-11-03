package dev.vessel.kernel.event.orchestration;

import dev.vessel.api.event.VesselCancellable;
import dev.vessel.kernel.event.EventExecutionPhase;
import java.lang.reflect.Method;
import java.util.Objects;
import org.bukkit.event.Cancellable;

/**
 * Bidirectional cancellation state bridge across VESSEL, Bukkit/Paper/Folia, Forge, and NeoForge
 * event representations ({@code ADR-0003} / {@code VES-EVENT-001}).
 *
 * <p>Unifies {@link VesselCancellable}, Bukkit {@link Cancellable}, and reflective Forge/NeoForge
 * {@code isCanceled()}/{@code setCanceled(boolean)} contracts while strictly enforcing read-only
 * immutability during the {@link EventExecutionPhase#MONITOR} inspection phase.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class CrossLoaderCancellationStateBridge {

    /**
     * Constructs a new cross-loader cancellation state bridge.
     */
    public CrossLoaderCancellationStateBridge() {
    }

    /**
     * Evaluates whether the supplied event object is currently marked as cancelled across any
     * supported loader contract.
     *
     * @param event event instance to inspect (nullable)
     * @return {@code true} if the event is cancelled, {@code false} if uncancelled or {@code null}
     */
    public boolean isEventCancelled(Object event) {
        if (event == null) {
            return false;
        }
        if (event instanceof VesselCancellable vc) {
            return vc.isCancelled();
        }
        if (event instanceof Cancellable bc) {
            return bc.isCancelled();
        }
        Boolean forgeCancelled = invokeZeroArgBooleanAccessor(event, "isCanceled");
        if (forgeCancelled != null) {
            return forgeCancelled;
        }
        Boolean genericCancelled = invokeZeroArgBooleanAccessor(event, "isCancelled");
        return genericCancelled != null && genericCancelled;
    }

    /**
     * Updates the cancellation state on the supplied event while guarding against illegal
     * mutations during {@link EventExecutionPhase#MONITOR}.
     *
     * @param event        non-null event instance to mutate
     * @param cancelled    desired cancellation state
     * @param currentPhase active execution phase (nullable)
     * @throws IllegalStateException if attempting to mutate cancellation state during {@link EventExecutionPhase#MONITOR}
     */
    public void setEventCancelled(Object event, boolean cancelled, EventExecutionPhase currentPhase) {
        Objects.requireNonNull(event, "event must not be null");
        if (currentPhase == EventExecutionPhase.MONITOR && isEventCancelled(event) != cancelled) {
            throw new IllegalStateException(
                    "ADR-0003 Violation: Event cancellation state cannot be mutated during MONITOR phase");
        }
        boolean handledByInterface = false;
        if (event instanceof VesselCancellable vc) {
            vc.setCancelled(cancelled);
            handledByInterface = true;
        }
        if (event instanceof Cancellable bc) {
            bc.setCancelled(cancelled);
            handledByInterface = true;
        }
        invokeBooleanMutator(event, "setCanceled", cancelled);
        if (!handledByInterface) {
            invokeBooleanMutator(event, "setCancelled", cancelled);
        }
    }

    /**
     * Synchronizes the cancellation state from {@code sourceEvent} onto {@code targetEvent}
     * under the constraints of {@code currentPhase}.
     *
     * @param sourceEvent  source event from which cancellation state is read (nullable)
     * @param targetEvent  non-null target event to receive the synchronized cancellation state
     * @param currentPhase active execution phase (nullable)
     */
    public void synchronizeCancellation(Object sourceEvent, Object targetEvent, EventExecutionPhase currentPhase) {
        boolean cancelled = isEventCancelled(sourceEvent);
        setEventCancelled(targetEvent, cancelled, currentPhase);
    }

    private Boolean invokeZeroArgBooleanAccessor(Object target, String methodName) {
        Method method = findMethod(target.getClass(), methodName);
        if (method == null) {
            return null;
        }
        try {
            method.setAccessible(true);
            Object result = method.invoke(target);
            if (result instanceof Boolean flag) {
                return flag;
            }
        } catch (ReflectiveOperationException ignored) {
            // Event does not expose an accessible boolean cancellation accessor
        }
        return null;
    }

    private void invokeBooleanMutator(Object target, String methodName, boolean value) {
        Method method = findMethod(target.getClass(), methodName, boolean.class);
        if (method == null) {
            return;
        }
        try {
            method.setAccessible(true);
            method.invoke(target, value);
        } catch (ReflectiveOperationException ignored) {
            // Event does not expose an accessible boolean cancellation mutator
        }
    }

    private Method findMethod(Class<?> type, String methodName, Class<?>... parameterTypes) {
        try {
            return type.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            Class<?> current = type;
            while (current != null && current != Object.class) {
                try {
                    return current.getDeclaredMethod(methodName, parameterTypes);
                } catch (NoSuchMethodException next) {
                    current = current.getSuperclass();
                }
            }
            return null;
        }
    }
}
