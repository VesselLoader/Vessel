package dev.vessel.kernel.transform.mixin;

import java.util.Objects;

/**
 * Immutable carrier representing a parsed Mixin annotation from bytecode inspection.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record ParsedMixinAnnotation(
        String annotationDescriptor,
        String targetMethod,
        String targetDescriptor,
        String injectionPointAt,
        String targetOwner,
        int ordinal,
        boolean cancellable
) {
    public ParsedMixinAnnotation {
        Objects.requireNonNull(annotationDescriptor, "annotationDescriptor cannot be null");
        Objects.requireNonNull(targetMethod, "targetMethod cannot be null");
        Objects.requireNonNull(injectionPointAt, "injectionPointAt cannot be null");
    }
}
