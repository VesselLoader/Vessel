package org.spongepowered.asm.mixin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Canonical SpongePowered Mixin unique member annotation
 * ({@code org.spongepowered.asm.mixin.Unique}).
 *
 * <p>Marks a mixin field, method, or interface as uniquely owned by the mixin so that
 * name collisions with the target class are avoided.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
public @interface Unique {

    /**
     * Whether colliding methods should be silently renamed rather than overriding.
     *
     * @return {@code true} if silent renaming is enabled
     */
    boolean silent() default false;
}
