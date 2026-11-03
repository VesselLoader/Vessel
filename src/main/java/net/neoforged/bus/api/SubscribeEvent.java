package net.neoforged.bus.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method on a registered NeoForge event listener class or instance as an event subscriber
 * ({@code net.neoforged.bus.api.SubscribeEvent}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubscribeEvent {

    /**
     * Execution priority of the subscribed event handler method.
     *
     * @return listener priority, defaulting to {@link EventPriority#NORMAL}
     */
    EventPriority priority() default EventPriority.NORMAL;

    /**
     * Whether this subscriber should still receive events that have been cancelled by an earlier listener.
     *
     * @return {@code true} to receive cancelled events, {@code false} otherwise
     */
    boolean receiveCanceled() default false;
}
