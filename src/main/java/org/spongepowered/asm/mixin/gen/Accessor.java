package org.spongepowered.asm.mixin.gen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * SpongePowered Mixin field accessor annotation ({@code org.spongepowered.asm.mixin.gen.Accessor})
 * declaring a synthetic getter or setter method for a target class field.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Accessor {

    /**
     * Explicit target field name; inferred from the accessor method name if empty.
     *
     * @return target field name
     */
    String value() default "";

    /**
     * Whether to remap the target field reference through the active obfuscation mapping table.
     *
     * @return {@code true} if remapping is enabled
     */
    boolean remap() default true;
}
