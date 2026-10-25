package dev.vessel.kernel.render;

/**
 * Standard downmixing coefficient profile for combining multichannel surround sound into mono/stereo.
 * Adheres to ITU-R BS.775 international broadcast recommendations.
 *
 * @param frontGain attenuation coefficient for Left and Right channels (standard: 0.7071 = 1 / sqrt(2))
 * @param centerGain attenuation coefficient for Center channel (standard: 1.0 or 0.7071)
 * @param surroundGain attenuation coefficient for Surround channels (standard: 0.5)
 * @param lfeGain attenuation coefficient for Subwoofer channel (standard: 0.0 to prevent low-end clipping)
 * @param normalizePeak whether output samples should be peak-normalized to prevent clipping [-1.0, 1.0]
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record AudioDownmixProfile(
        double frontGain,
        double centerGain,
        double surroundGain,
        double lfeGain,
        boolean normalizePeak
) {
    public AudioDownmixProfile {
        if (frontGain < 0.0 || centerGain < 0.0 || surroundGain < 0.0 || lfeGain < 0.0) {
            throw new IllegalArgumentException("Gain coefficients must be non-negative");
        }
    }

    /**
     * Standard ITU-R BS.775 downmix profile.
     */
    public static final AudioDownmixProfile ITU_R_BS_775 = new AudioDownmixProfile(
            0.70710678, // 1 / sqrt(2)
            1.00000000,
            0.50000000,
            0.00000000,
            true
    );
}
