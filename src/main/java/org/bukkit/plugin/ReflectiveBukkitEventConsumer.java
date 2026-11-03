package org.bukkit.plugin;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Consumer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Listener;

/**
 * Standalone reflective consumer adapter that invokes a single Bukkit {@link Listener} event
 * handler method while enforcing {@code ignoreCancelled} semantics and {@code MONITOR}-phase
 * read-only cancellation preservation per ADR-0003.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ReflectiveBukkitEventConsumer implements Consumer<Object> {

    private final Listener listener;
    private final Method method;
    private final Class<?> paramType;
    private final boolean ignoreCancelled;
    private final boolean monitorPhase;

    /**
     * Constructs a reflective consumer bound to a target {@link Listener} method.
     *
     * @param listener        the listener instance
     * @param method          the reflective handler method
     * @param paramType       the expected event parameter class
     * @param ignoreCancelled {@code true} if cancelled events should be skipped
     * @param monitorPhase    {@code true} if registered at {@code MONITOR} priority
     */
    public ReflectiveBukkitEventConsumer(
            Listener listener,
            Method method,
            Class<?> paramType,
            boolean ignoreCancelled,
            boolean monitorPhase) {
        this.listener = Objects.requireNonNull(listener, "listener must not be null");
        this.method = Objects.requireNonNull(method, "method must not be null");
        this.paramType = Objects.requireNonNull(paramType, "paramType must not be null");
        this.ignoreCancelled = ignoreCancelled;
        this.monitorPhase = monitorPhase;
        this.method.setAccessible(true);
    }

    @Override
    public void accept(Object eventObj) {
        if (!paramType.isInstance(eventObj)) {
            return;
        }
        boolean wasCancelled = (eventObj instanceof Cancellable c) && c.isCancelled();
        if (ignoreCancelled && wasCancelled) {
            return;
        }
        try {
            method.invoke(listener, eventObj);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to invoke Bukkit event handler " + method, e);
        } finally {
            if (monitorPhase && eventObj instanceof Cancellable c && c.isCancelled() != wasCancelled) {
                c.setCancelled(wasCancelled);
            }
        }
    }
}
