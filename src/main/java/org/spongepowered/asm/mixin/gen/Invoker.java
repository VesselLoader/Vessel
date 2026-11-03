package org.spongepowered.asm.mixin.gen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * SpongePowered Mixin method invoker annotation ({@code org.spongepowered.asm.mixin.gen.Invoker})
 * declaring a synthetic proxy method for invoking a private or package-private target method.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Invoker {

    /**
     * Explicit target method name; inferred from the invoker method name if empty.
     *
     * @return target method name
     */
    String value() default "";

    /**
     * Whether to remap the target method reference through the active obfuscation mapping table.
     *
     * @return {@code true} if remapping is enabled
     */
    boolean remap() default true;
}
