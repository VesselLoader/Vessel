package dev.vessel.kernel.render;

/**
 * Immutable spatial coordinate and sound propagation settings of an audio emitter.
 *
 * @param posX world X coordinate
 * @param posY world Y coordinate
 * @param posZ world Z coordinate
 * @param maxAudibleRange maximum distance in meters before complete sound cutoff
 * @param rolloffFactor logarithmic distance attenuation multiplier (typically 1.0)
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record SpatialSoundSourcePosition(
        double posX,
        double posY,
        double posZ,
        double maxAudibleRange,
        double rolloffFactor
) {
    public SpatialSoundSourcePosition {
        if (maxAudibleRange <= 0.0) {
            throw new IllegalArgumentException("maxAudibleRange must be strictly positive: " + maxAudibleRange);
        }
        if (rolloffFactor < 0.0) {
            throw new IllegalArgumentException("rolloffFactor must be non-negative: " + rolloffFactor);
        }
    }
}
