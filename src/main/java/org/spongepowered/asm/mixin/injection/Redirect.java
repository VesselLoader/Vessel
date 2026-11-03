package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Canonical SpongePowered Mixin call/field redirection injector annotation
 * ({@code org.spongepowered.asm.mixin.injection.Redirect}).
 *
 * <p>Specifies that a method invocation or field access matched by {@link #at()} inside the
 * target method(s) should be redirected to the annotated mixin handler method.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Redirect {

    /**
     * Target method selectors (names and optional descriptors).
     *
     * @return array of target method selectors
     */
    String[] method() default {};

    /**
     * Injection point selector identifying the specific call or field access to redirect.
     *
     * @return {@link At} selector
     */
    At at();

    /**
     * Whether the target method reference should be remapped.
     *
     * @return {@code true} if remapping is enabled
     */
    boolean remap() default true;
}
