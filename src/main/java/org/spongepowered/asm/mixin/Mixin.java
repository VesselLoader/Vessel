package org.spongepowered.asm.mixin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Canonical SpongePowered Mixin class declaration annotation
 * ({@code org.spongepowered.asm.mixin.Mixin}).
 *
 * <p>Identifies a class as a Mixin targeting one or more Minecraft or library classes
 * across Fabric, Quilt, Forge, and NeoForge mods.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Mixin {

    /**
     * Target classes for this mixin.
     *
     * @return array of target class tokens
     */
    Class<?>[] value() default {};

    /**
     * Target class names for private or inaccessible targets.
     *
     * @return array of target binary or internal class names
     */
    String[] targets() default {};

    /**
     * Relative application priority for this mixin.
     *
     * @return mixin priority (defaults to {@code 1000})
     */
    int priority() default 1000;

    /**
     * Whether symbol references in this mixin should be remapped at runtime.
     *
     * @return {@code true} if remapping is enabled
     */
    boolean remap() default true;
}
