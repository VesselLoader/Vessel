package com.velocitypowered.api.plugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Velocity plugin descriptor annotation ({@code com.velocitypowered.api.plugin.Plugin}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Plugin {

    /**
     * Unique identifier for the plugin.
     *
     * @return plugin identifier
     */
    String id();

    /**
     * Human-readable plugin name.
     *
     * @return plugin name
     */
    String name() default "";

    /**
     * Plugin version string.
     *
     * @return plugin version
     */
    String version() default "";

    /**
     * Brief plugin description.
     *
     * @return plugin description
     */
    String description() default "";

    /**
     * Authors who created this plugin.
     *
     * @return authors array
     */
    String[] authors() default {};
}
