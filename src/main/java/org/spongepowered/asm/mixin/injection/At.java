package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Canonical SpongePowered Mixin injection point selector annotation
 * ({@code org.spongepowered.asm.mixin.injection.At}).
 *
 * <p>Specifies the bytecode location (e.g., {@code "HEAD"}, {@code "RETURN"}, {@code "INVOKE"})
 * within a target method where an {@link Inject} or {@link Redirect} handler is woven.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface At {

    /**
     * Injection point code (e.g., {@code "HEAD"}, {@code "RETURN"}, {@code "TAIL"}, {@code "INVOKE"}).
     *
     * @return injection point type identifier
     */
    String value();

    /**
     * Target member descriptor for instruction-targeted injection points such as {@code "INVOKE"}
     * or {@code "FIELD"}.
     *
     * @return target member reference or empty string
     */
    String target() default "";

    /**
     * Zero-based ordinal index of the matched instruction, or {@code -1} to match all occurrences.
     *
     * @return target instruction ordinal
     */
    int ordinal() default -1;

    /**
     * Whether the {@link #target()} descriptor should be remapped.
     *
     * @return {@code true} if remapping is enabled
     */
    boolean remap() default true;
}
