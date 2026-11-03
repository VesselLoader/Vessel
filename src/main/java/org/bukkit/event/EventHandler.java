package org.bukkit.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to mark methods on a {@link Listener} class that should receive Bukkit/Paper
 * {@link Event} dispatches through the VESSEL Sovereign Microkernel.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventHandler {

    /**
     * Defines the execution priority of this event handler relative to other handlers.
     *
     * @return the handler {@link EventPriority}, defaulting to {@link EventPriority#NORMAL}
     */
    EventPriority priority() default EventPriority.NORMAL;

    /**
     * Defines whether this handler should be skipped if the target {@link Cancellable} event has
     * already been cancelled by an earlier priority tier.
     *
     * @return {@code true} if cancelled events should be ignored
     */
    boolean ignoreCancelled() default false;
}
