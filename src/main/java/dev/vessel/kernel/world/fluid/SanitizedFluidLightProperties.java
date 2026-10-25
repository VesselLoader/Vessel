package dev.vessel.kernel.world.fluid;

import java.util.Objects;

/**
 * Immutable sanitized light parameters for modded custom fluids guaranteed safe for Starlight BFS queues.
 *
 * @param fluidId     identifier of the fluid (e.g. "mekanism:heavy_water", "create:chocolate")
 * @param luminance   safe light emission intensity clamped within [0, 15]
 * @param opacity     safe light attenuation opacity clamped within [0, 15]
 * @param wasClamped  true if anomalous/out-of-bounds input values required correction
 * @param auditReason diagnostic explanation for why sanitization clamped or adjusted the values
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record SanitizedFluidLightProperties(
        String fluidId,
        int luminance,
        int opacity,
        boolean wasClamped,
        String auditReason
) {
    /**
     * Validates sanitized invariants.
     */
    public SanitizedFluidLightProperties {
        Objects.requireNonNull(fluidId, "fluidId cannot be null");
        Objects.requireNonNull(auditReason, "auditReason cannot be null");
        if (luminance < 0 || luminance > 15) {
            throw new IllegalArgumentException("luminance must be in range [0, 15]: " + luminance);
        }
        if (opacity < 0 || opacity > 15) {
            throw new IllegalArgumentException("opacity must be in range [0, 15]: " + opacity);
        }
    }
}
