package dev.vessel.kernel.render;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable atomic element of a Java Edition block model (from/to bounding box and faces).
 *
 * @param from [x, y, z] start coordinates (0.0 to 16.0)
 * @param to [x, y, z] end coordinates (0.0 to 16.0)
 * @param faces map of directional face names ("north", "south", "east", "west", "up", "down") to face descriptors
 * @param rotationOrigin rotation center point [x, y, z] (nullable)
 * @param rotationAxis rotation axis "x", "y", or "z" (nullable)
 * @param rotationAngle rotation angle in degrees (-45.0 to 45.0)
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record JavaBlockModelElement(
        float[] from,
        float[] to,
        Map<String, JavaModelFace> faces,
        float[] rotationOrigin,
        String rotationAxis,
        float rotationAngle
) {
    public JavaBlockModelElement {
        Objects.requireNonNull(from, "from cannot be null");
        Objects.requireNonNull(to, "to cannot be null");
        if (from.length != 3 || to.length != 3) {
            throw new IllegalArgumentException("from and to vectors must have length 3");
        }
        faces = faces != null ? Collections.unmodifiableMap(faces) : Collections.emptyMap();
        if (rotationOrigin != null && rotationOrigin.length != 3) {
            throw new IllegalArgumentException("rotationOrigin must have length 3");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JavaBlockModelElement that)) return false;
        return Float.compare(that.rotationAngle, rotationAngle) == 0 &&
                Arrays.equals(from, that.from) &&
                Arrays.equals(to, that.to) &&
                Objects.equals(faces, that.faces) &&
                Arrays.equals(rotationOrigin, that.rotationOrigin) &&
                Objects.equals(rotationAxis, that.rotationAxis);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(faces, rotationAxis, rotationAngle);
        result = 31 * result + Arrays.hashCode(from);
        result = 31 * result + Arrays.hashCode(to);
        result = 31 * result + Arrays.hashCode(rotationOrigin);
        return result;
    }
}
