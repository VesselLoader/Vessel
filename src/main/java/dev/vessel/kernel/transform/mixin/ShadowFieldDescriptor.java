package dev.vessel.kernel.transform.mixin;

import java.util.Objects;

/**
 * Immutable carrier describing a field targeted by a Mixin @Shadow annotation.
 *
 * @param targetClass owner class internal name
 * @param fieldName expected field name
 * @param fieldDescriptor expected JVM type descriptor
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record ShadowFieldDescriptor(
        String targetClass,
        String fieldName,
        String fieldDescriptor
) {
    public ShadowFieldDescriptor {
        Objects.requireNonNull(targetClass, "targetClass cannot be null");
        Objects.requireNonNull(fieldName, "fieldName cannot be null");
        Objects.requireNonNull(fieldDescriptor, "fieldDescriptor cannot be null");
    }
}
