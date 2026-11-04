package dev.vessel.kernel.resourcepack;

/**
 * Metadata descriptor for an Ogg Vorbis sound asset.
 *
 * @param sizeBytes file size in bytes
 * @param durationSeconds approximate or exact audio duration in seconds
 * @param sampleRate audio sampling rate in Hz (e.g. 44100 or 48000)
 * @param channels number of audio channels (1 for mono, 2 for stereo)
 * @param shouldStream whether the sound must have {@code "stream": true} set in sounds.json
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record VesselSoundMetadata(
        long sizeBytes,
        double durationSeconds,
        int sampleRate,
        int channels,
        boolean shouldStream
) {

    public VesselSoundMetadata {
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes cannot be negative: " + sizeBytes);
        }
        if (durationSeconds < 0.0) {
            throw new IllegalArgumentException("durationSeconds cannot be negative: " + durationSeconds);
        }
    }
}
