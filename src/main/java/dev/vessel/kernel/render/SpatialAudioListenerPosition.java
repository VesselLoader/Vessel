package dev.vessel.kernel.render;

/**
 * Immutable spatial coordinate and orientation state of an audio listener (player head).
 *
 * @param posX world X coordinate
 * @param posY world Y coordinate
 * @param posZ world Z coordinate
 * @param yaw listener horizontal rotation in degrees (-180.0 to 180.0)
 * @param pitch listener vertical pitch in degrees (-90.0 to 90.0)
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record SpatialAudioListenerPosition(
        double posX,
        double posY,
        double posZ,
        double yaw,
        double pitch
) {
}
