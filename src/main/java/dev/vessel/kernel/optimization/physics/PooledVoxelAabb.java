package dev.vessel.kernel.optimization.physics;

import java.util.Objects;

/**
 * Reusable mutable Axis-Aligned Bounding Box (AABB) object supporting zero-allocation pooling.
 * <p>
 * Implements {@link AutoCloseable} to enable ergonomic and safe usage within try-with-resources blocks.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class PooledVoxelAabb implements AutoCloseable {

    private final VoxelShapeAabbCachePool owningPool;
    private double minX;
    private double minY;
    private double minZ;
    private double maxX;
    private double maxY;
    private double maxZ;
    private boolean inUse;

    /**
     * Constructs a PooledVoxelAabb associated with an owning cache pool.
     *
     * @param owningPool pool instance responsible for managing this object
     */
    public PooledVoxelAabb(VoxelShapeAabbCachePool owningPool) {
        this.owningPool = Objects.requireNonNull(owningPool, "owningPool cannot be null");
        this.inUse = false;
    }

    /**
     * Updates the bounding coordinates of this AABB.
     *
     * @param minX minimum X boundary
     * @param minY minimum Y boundary
     * @param minZ minimum Z boundary
     * @param maxX maximum X boundary
     * @param maxY maximum Y boundary
     * @param maxZ maximum Z boundary
     */
    public void set(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
        this.inUse = true;
    }

    /**
     * Tests spatial intersection against another bounding box specified by primitive coordinates.
     *
     * @param oMinX other min X
     * @param oMinY other min Y
     * @param oMinZ other min Z
     * @param oMaxX other max X
     * @param oMaxY other max Y
     * @param oMaxZ other max Z
     * @return true if bounding boxes intersect
     */
    public boolean intersects(double oMinX, double oMinY, double oMinZ, double oMaxX, double oMaxY, double oMaxZ) {
        return this.minX < oMaxX && this.maxX > oMinX &&
               this.minY < oMaxY && this.maxY > oMinY &&
               this.minZ < oMaxZ && this.maxZ > oMinZ;
    }

    /**
     * Tests spatial intersection against another {@link PooledVoxelAabb}.
     *
     * @param other target bounding box
     * @return true if bounding boxes intersect
     */
    public boolean intersects(PooledVoxelAabb other) {
        Objects.requireNonNull(other, "other cannot be null");
        return intersects(other.minX, other.minY, other.minZ, other.maxX, other.maxY, other.maxZ);
    }

    /**
     * Checks if a 3D point is contained within this bounding box.
     *
     * @param x coordinate X
     * @param y coordinate Y
     * @param z coordinate Z
     * @return true if point is enclosed
     */
    public boolean contains(double x, double y, double z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    /**
     * Resets coordinates to zero and marks object as inactive.
     */
    public void reset() {
        this.minX = 0.0;
        this.minY = 0.0;
        this.minZ = 0.0;
        this.maxX = 0.0;
        this.maxY = 0.0;
        this.maxZ = 0.0;
        this.inUse = false;
    }

    @Override
    public void close() {
        if (inUse) {
            owningPool.release(this);
        }
    }

    public double getMinX() {
        return minX;
    }

    public double getMinY() {
        return minY;
    }

    public double getMinZ() {
        return minZ;
    }

    public double getMaxX() {
        return maxX;
    }

    public double getMaxY() {
        return maxY;
    }

    public double getMaxZ() {
        return maxZ;
    }

    public boolean isInUse() {
        return inUse;
    }

    void setInUse(boolean inUse) {
        this.inUse = inUse;
    }
}
