package dev.vessel.kernel.transform.mixin;

import java.util.Objects;

/**
 * Immutable report summarizing the validation of a Mixin @Shadow field target.
 *
 * @param descriptor the shadow target requested
 * @param status the validation status code
 * @param detail descriptive diagnosis message
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record ShadowValidationResult(
        ShadowFieldDescriptor descriptor,
        ShadowValidationStatus status,
        String detail
) {
    public ShadowValidationResult {
        Objects.requireNonNull(descriptor, "descriptor cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        Objects.requireNonNull(detail, "detail cannot be null");
    }

    public boolean isValid() {
        return status == ShadowValidationStatus.VALID;
    }
}
