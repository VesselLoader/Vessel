package dev.vessel.kernel.render;

import java.util.Objects;

/**
 * Immutable output of a 3D audio spatial downmix calculation.
 *
 * @param monoSamples downmixed single-channel PCM audio samples
 * @param volumeGain distance attenuation multiplier [0.0, 1.0]
 * @param distanceMeters linear Euclidean distance to listener
 * @param panAzimuthDegrees horizontal sound azimuth relative to listener head (-180.0 to 180.0)
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record SpatialAudioDownmixResult(
        float[] monoSamples,
        double volumeGain,
        double distanceMeters,
        double panAzimuthDegrees
) {
    public SpatialAudioDownmixResult {
        Objects.requireNonNull(monoSamples, "monoSamples cannot be null");
        if (distanceMeters < 0.0) {
            throw new IllegalArgumentException("distanceMeters cannot be negative: " + distanceMeters);
        }
    }
}
