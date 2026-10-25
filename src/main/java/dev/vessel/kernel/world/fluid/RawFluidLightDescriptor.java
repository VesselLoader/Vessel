package dev.vessel.kernel.world.fluid;

import java.util.Objects;

/**
 * Raw unchecked light parameters reported by third-party modded fluids before sanitization.
 *
 * @param fluidId      identifier of the modded fluid
 * @param rawLuminance unchecked reported luminance value (may be negative, > 15, or corrupted)
 * @param rawOpacity   unchecked reported opacity value (may be negative, > 15, or corrupted)
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record RawFluidLightDescriptor(
        String fluidId,
        int rawLuminance,
        int rawOpacity
) {
    /**
     * Constructs a RawFluidLightDescriptor with non-null ID validation.
     */
    public RawFluidLightDescriptor {
        Objects.requireNonNull(fluidId, "fluidId cannot be null");
    }
}
