package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Canonical SpongePowered Mixin callback injector annotation
 * ({@code org.spongepowered.asm.mixin.injection.Inject}).
 *
 * <p>Specifies that the annotated mixin method should be injected as a callback into one or
 * more target methods at the specified {@link At} injection points.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Inject {

    /**
     * Target method selectors (names and optional descriptors).
     *
     * @return array of target method selectors
     */
    String[] method() default {};

    /**
     * Injection point selectors specifying where in the target method(s) to weave the callback.
     *
     * @return array of {@link At} injection point selectors
     */
    At[] at() default {};

    /**
     * Whether the injected callback is permitted to cancel the target method or override its return value.
     *
     * @return {@code true} if cancellable
     */
    boolean cancellable() default false;

    /**
     * Whether the target method reference should be remapped.
     *
     * @return {@code true} if remapping is enabled
     */
    boolean remap() default true;
}
