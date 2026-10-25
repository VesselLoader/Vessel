package dev.vessel.kernel.render;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable representation of a single face definition in a Java block model element.
 *
 * @param uv 4-element UV coordinates [u1, v1, u2, v2] in pixel space (0..16)
 * @param texture texture variable reference (e.g. "#all", "#side", "#layer0")
 * @param rotation face rotation angle in degrees (0, 90, 180, 270)
 * @param tintIndex color tint index (-1 if none)
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record JavaModelFace(
        float[] uv,
        String texture,
        int rotation,
        int tintIndex
) {
    public JavaModelFace {
        Objects.requireNonNull(texture, "texture cannot be null");
        if (uv != null && uv.length != 4) {
            throw new IllegalArgumentException("UV coordinates array must have length 4");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JavaModelFace that)) return false;
        return rotation == that.rotation &&
                tintIndex == that.tintIndex &&
                Arrays.equals(uv, that.uv) &&
                Objects.equals(texture, that.texture);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(texture, rotation, tintIndex);
        result = 31 * result + Arrays.hashCode(uv);
        return result;
    }

    @Override
    public String toString() {
        return "JavaModelFace[uv=" + Arrays.toString(uv) + ", texture=" + texture +
                ", rotation=" + rotation + ", tintIndex=" + tintIndex + "]";
    }
}
