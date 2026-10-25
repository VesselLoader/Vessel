package dev.vessel.kernel.optimization.physics;

/**
 * Functional callback interface for receiving zero-allocation collision hits
 * from {@link SpatialBoundingVolumeHierarchy}.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
@FunctionalInterface
public interface BvhCollisionConsumer {

    /**
     * Invoked when a bounding box intersection is detected.
     *
     * @param entityId candidate entity identifier
     * @param minX     bounding box minimum X
     * @param minY     bounding box minimum Y
     * @param minZ     bounding box minimum Z
     * @param maxX     bounding box maximum X
     * @param maxY     bounding box maximum Y
     * @param maxZ     bounding box maximum Z
     */
    void onCollision(int entityId, float minX, float minY, float minZ, float maxX, float maxY, float maxZ);
}
