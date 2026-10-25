package dev.vessel.kernel.display.model;

import java.util.Arrays;
import java.util.Objects;

/**
 * Compact memory-optimized 3D model mesh definition.
 * Uses contiguous primitive arrays for vertices, UV offsets, and indices to eliminate
 * object allocations and reduce JVM heap pressure during custom model rendering.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class CompactModelMeshDefinition {

    private final String modelId;
    private final float[] vertexCoordinates; // Flattened [x, y, z, x, y, z...]
    private final float[] uvCoordinates;     // Flattened [u, v, u, v...]
    private final int[] indices;             // Element draw indices

    /**
     * Constructs a new CompactModelMeshDefinition.
     *
     * @param modelId unique identifier for this model mesh
     * @param vertexCoordinates flattened vertex coordinates
     * @param uvCoordinates flattened texture UV coordinates
     * @param indices element face index buffer
     */
    public CompactModelMeshDefinition(
            String modelId,
            float[] vertexCoordinates,
            float[] uvCoordinates,
            int[] indices) {
        this.modelId = Objects.requireNonNull(modelId, "modelId cannot be null");
        this.vertexCoordinates = Objects.requireNonNull(vertexCoordinates, "vertexCoordinates cannot be null").clone();
        this.uvCoordinates = Objects.requireNonNull(uvCoordinates, "uvCoordinates cannot be null").clone();
        this.indices = Objects.requireNonNull(indices, "indices cannot be null").clone();

        if (this.vertexCoordinates.length % 3 != 0) {
            throw new IllegalArgumentException("vertexCoordinates length must be a multiple of 3 (x, y, z)");
        }
        if (this.uvCoordinates.length % 2 != 0) {
            throw new IllegalArgumentException("uvCoordinates length must be a multiple of 2 (u, v)");
        }
    }

    /**
     * Returns the unique model identifier.
     *
     * @return model identifier
     */
    public String getModelId() {
        return modelId;
    }

    /**
     * Returns total vertex count.
     *
     * @return count of 3D vertices
     */
    public int getVertexCount() {
        return vertexCoordinates.length / 3;
    }

    /**
     * Returns total element index count.
     *
     * @return index count
     */
    public int getIndexCount() {
        return indices.length;
    }

    /**
     * Computes the axis-aligned bounding box (AABB) extents of this mesh.
     *
     * @return float array [minX, minY, minZ, maxX, maxY, maxZ]
     */
    public float[] computeBoundingBox() {
        if (vertexCoordinates.length == 0) {
            return new float[]{0f, 0f, 0f, 0f, 0f, 0f};
        }

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < vertexCoordinates.length; i += 3) {
            float x = vertexCoordinates[i];
            float y = vertexCoordinates[i + 1];
            float z = vertexCoordinates[i + 2];

            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (z < minZ) minZ = z;

            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
            if (z > maxZ) maxZ = z;
        }

        return new float[]{minX, minY, minZ, maxX, maxY, maxZ};
    }

    public float[] getVertexCoordinatesCopy() {
        return vertexCoordinates.clone();
    }

    public float[] getUvCoordinatesCopy() {
        return uvCoordinates.clone();
    }

    public int[] getIndicesCopy() {
        return indices.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CompactModelMeshDefinition that)) return false;
        return modelId.equals(that.modelId)
                && Arrays.equals(vertexCoordinates, that.vertexCoordinates)
                && Arrays.equals(uvCoordinates, that.uvCoordinates)
                && Arrays.equals(indices, that.indices);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelId, Arrays.hashCode(vertexCoordinates), Arrays.hashCode(uvCoordinates), Arrays.hashCode(indices));
    }
}
