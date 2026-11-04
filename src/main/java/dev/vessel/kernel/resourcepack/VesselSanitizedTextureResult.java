package dev.vessel.kernel.resourcepack;

import java.util.Arrays;
import java.util.Objects;

/**
 * Result of sanitizing a texture asset for atlas packing and mobile GPU rendering.
 *
 * @param sanitizedPngData PNG byte payload with transparent padding applied
 * @param originalWidth original width in pixels
 * @param originalHeight original height in pixels
 * @param paddedWidth sanitized width including padding
 * @param paddedHeight sanitized height including padding
 * @param originallyPowerOfTwo whether the input was a power of two before padding
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record VesselSanitizedTextureResult(
        byte[] sanitizedPngData,
        int originalWidth,
        int originalHeight,
        int paddedWidth,
        int paddedHeight,
        boolean originallyPowerOfTwo
) {

    public VesselSanitizedTextureResult {
        Objects.requireNonNull(sanitizedPngData, "sanitizedPngData cannot be null");
        sanitizedPngData = Arrays.copyOf(sanitizedPngData, sanitizedPngData.length);
    }

    @Override
    public byte[] sanitizedPngData() {
        return Arrays.copyOf(sanitizedPngData, sanitizedPngData.length);
    }
}
