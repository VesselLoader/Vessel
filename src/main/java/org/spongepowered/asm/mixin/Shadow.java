package org.spongepowered.asm.mixin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Canonical SpongePowered Mixin shadow member annotation
 * ({@code org.spongepowered.asm.mixin.Shadow}).
 *
 * <p>Marks a field or method in a {@link Mixin} class as a placeholder mirroring a corresponding
 * member on the target class.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface Shadow {

    /**
     * Optional prefix stripped from the declared member name when resolving the target member.
     *
     * @return shadow prefix (defaults to {@code "shadow$"})
     */
    String prefix() default "shadow$";

    /**
     * Whether this shadowed member should be remapped via the active mapping table.
     *
     * @return {@code true} if remapping is enabled
     */
    boolean remap() default true;

    /**
     * Candidate alias names for resolving the target member.
     *
     * @return array of alias names
     */
    String[] aliases() default {};
}
