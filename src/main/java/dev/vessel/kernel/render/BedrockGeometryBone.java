package dev.vessel.kernel.render;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Bedrock edition geometry bone definition, containing cubes and pivot hierarchy.
 *
 * @param name unique bone identifier
 * @param parent parent bone name (nullable for root bones)
 * @param pivot 3-element pivot coordinate [px, py, pz]
 * @param rotation 3-element initial rotation [rx, ry, rz]
 * @param cubes list of cubes attached to this bone
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record BedrockGeometryBone(
        String name,
        String parent,
        float[] pivot,
        float[] rotation,
        List<BedrockGeometryCube> cubes
) {
    public BedrockGeometryBone {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(pivot, "pivot cannot be null");
        if (pivot.length != 3) {
            throw new IllegalArgumentException("pivot must have length 3");
        }
        cubes = cubes != null ? Collections.unmodifiableList(cubes) : Collections.emptyList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BedrockGeometryBone that)) return false;
        return Objects.equals(name, that.name) &&
                Objects.equals(parent, that.parent) &&
                Arrays.equals(pivot, that.pivot) &&
                Arrays.equals(rotation, that.rotation) &&
                Objects.equals(cubes, that.cubes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(name, parent, cubes);
        result = 31 * result + Arrays.hashCode(pivot);
        result = 31 * result + Arrays.hashCode(rotation);
        return result;
    }
}
