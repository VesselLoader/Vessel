package com.llamalad7.mixinextras.injector;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.spongepowered.asm.mixin.injection.At;

/**
 * MixinExtras return-value modifier annotation
 * ({@code com.llamalad7.mixinextras.injector.ModifyReturnValue}) allowing composable transformation
 * of a target method's return value without cancelling control flow.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ModifyReturnValue {

    /**
     * Target method selectors.
     *
     * @return array of target method descriptors
     */
    String[] method() default {};

    /**
     * Injection point selectors targeting {@code RETURN} instructions.
     *
     * @return array of {@link At} selectors
     */
    At[] at() default {};

    /**
     * Whether to remap target descriptors through the active obfuscation mapping table.
     *
     * @return {@code true} if remapping is enabled
     */
    boolean remap() default true;
}
