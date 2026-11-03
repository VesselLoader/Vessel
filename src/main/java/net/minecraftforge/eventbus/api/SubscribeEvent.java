package net.minecraftforge.eventbus.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Canonical Minecraft Forge event listener method annotation
 * ({@code net.minecraftforge.eventbus.api.SubscribeEvent}).
 *
 * <p>Marks a 1-argument method on an object or class registered with {@link IEventBus#register(Object)}
 * for automatic event dispatch.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubscribeEvent {

    /**
     * Returns the execution priority for this event subscriber method.
     *
     * @return execution priority (defaults to {@link EventPriority#NORMAL})
     */
    EventPriority priority() default EventPriority.NORMAL;

    /**
     * Returns whether this subscriber should still receive events that have been cancelled.
     *
     * @return {@code true} to receive cancelled events, {@code false} otherwise
     */
    boolean receiveCanceled() default false;
}
