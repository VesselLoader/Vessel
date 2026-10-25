package dev.vessel.kernel.render;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Bedrock edition geometry cube definition.
 *
 * @param origin cube origin coordinate [x, y, z] in Bedrock model space
 * @param size cube extent [sizeX, sizeY, sizeZ]
 * @param rotation cube local rotation angles [rotX, rotY, rotZ] in degrees
 * @param pivot cube local pivot point [pX, pY, pZ]
 * @param faceUvs map of face names to 4-element UV arrays [u1, v1, u2, v2]
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record BedrockGeometryCube(
        float[] origin,
        float[] size,
        float[] rotation,
        float[] pivot,
        Map<String, float[]> faceUvs
) {
    public BedrockGeometryCube {
        Objects.requireNonNull(origin, "origin cannot be null");
        Objects.requireNonNull(size, "size cannot be null");
        if (origin.length != 3 || size.length != 3) {
            throw new IllegalArgumentException("origin and size vectors must have length 3");
        }
        faceUvs = faceUvs != null ? Collections.unmodifiableMap(faceUvs) : Collections.emptyMap();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BedrockGeometryCube that)) return false;
        return Arrays.equals(origin, that.origin) &&
                Arrays.equals(size, that.size) &&
                Arrays.equals(rotation, that.rotation) &&
                Arrays.equals(pivot, that.pivot) &&
                Objects.equals(faceUvs, that.faceUvs);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(origin);
        result = 31 * result + Arrays.hashCode(size);
        result = 31 * result + Arrays.hashCode(rotation);
        result = 31 * result + Arrays.hashCode(pivot);
        result = 31 * result + Objects.hashCode(faceUvs);
        return result;
    }
}
