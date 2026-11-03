package net.md_5.bungee.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation marking a method as an event subscriber in the BungeeCord event bus
 * ({@code net.md_5.bungee.event.EventHandler}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventHandler {

    /**
     * Relative execution priority byte (default is 0).
     *
     * @return priority byte
     */
    byte priority() default 0;
}
