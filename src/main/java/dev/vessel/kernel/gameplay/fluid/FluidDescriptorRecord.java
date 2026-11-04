package dev.vessel.kernel.gameplay.fluid;

/**
 * Immutable descriptor representing fluid physical properties (viscosity, density, temperature, luminance).
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public record FluidDescriptorRecord(
        String fluidIdentifier,
        int viscosity,
        int density,
        int temperature,
        int luminance,
        boolean canConvertToSource
) {
}
