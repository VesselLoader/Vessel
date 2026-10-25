package dev.vessel.kernel.display.model;

import java.util.Objects;

/**
 * Immutable snapshot of the Blood Cauldron state for telemetry and synchronization.
 *
 * @param blockEntityId entity or block entity ID
 * @param fillFraction fluid level [0.0, 1.0]
 * @param temperature operating temperature tier
 * @param fluidColorHex RGB color integer
 * @param boiling whether cauldron is currently boiling
 * @param heatSourcePresent whether active heat source exists below
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record BloodCauldronSnapshot(
        int blockEntityId,
        float fillFraction,
        CauldronTemperatureState temperature,
        int fluidColorHex,
        boolean boiling,
        boolean heatSourcePresent
) {
    public BloodCauldronSnapshot {
        Objects.requireNonNull(temperature, "temperature cannot be null");
    }
}
