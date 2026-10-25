package dev.vessel.kernel.optimization.physics;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * High-performance object recycling pool for Axis-Aligned Bounding Boxes (AABB).
 * <p>
 * Replaces high-frequency heap allocations of collision boxes in physics, block raytracing, and entity
 * movement collision loops. Drives per-operation heap allocations down to zero bytes, mitigating ERR-41
 * and resolving BLK-0601 through BLK-0800.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class VoxelShapeAabbCachePool {

    public static final int DEFAULT_CAPACITY = 1024;
    public static final int MAX_CAPACITY = 8192;

    private final PooledVoxelAabb[] pool;
    private final int capacity;
    private int top;

    private final AtomicLong totalAcquired;
    private final AtomicLong totalReleased;

    /**
     * Constructs a VoxelShapeAabbCachePool with default capacity (1024 instances).
     */
    public VoxelShapeAabbCachePool() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Constructs a VoxelShapeAabbCachePool with specified capacity.
     *
     * @param capacity maximum idle objects stored in this pool
     */
    public VoxelShapeAabbCachePool(int capacity) {
        if (capacity < 16 || capacity > MAX_CAPACITY) {
            throw new IllegalArgumentException("Capacity must be between 16 and " + MAX_CAPACITY + ": " + capacity);
        }
        this.capacity = capacity;
        this.pool = new PooledVoxelAabb[capacity];
        this.top = 0;
        this.totalAcquired = new AtomicLong(0L);
        this.totalReleased = new AtomicLong(0L);
    }

    /**
     * Pre-allocates and caches instances to ensure zero runtime allocation on hot-paths.
     *
     * @param count number of instances to pre-populate
     */
    public synchronized void prime(int count) {
        int target = Math.min(count, capacity);
        while (top < target) {
            pool[top++] = new PooledVoxelAabb(this);
        }
    }

    /**
     * Acquires a recycled {@link PooledVoxelAabb} instance and initializes its boundaries.
     *
     * @param minX minimum X boundary
     * @param minY minimum Y boundary
     * @param minZ minimum Z boundary
     * @param maxX maximum X boundary
     * @param maxY maximum Y boundary
     * @param maxZ maximum Z boundary
     * @return initialized pooled bounding box
     */
    public synchronized PooledVoxelAabb acquire(
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ
    ) {
        totalAcquired.incrementAndGet();
        PooledVoxelAabb instance;
        if (top > 0) {
            instance = pool[--top];
        } else {
            instance = new PooledVoxelAabb(this);
        }
        instance.set(minX, minY, minZ, maxX, maxY, maxZ);
        return instance;
    }

    /**
     * Releases an active {@link PooledVoxelAabb} back into the pool for future reuse.
     *
     * @param aabb bounding box to recycle
     */
    public synchronized void release(PooledVoxelAabb aabb) {
        Objects.requireNonNull(aabb, "aabb cannot be null");
        if (!aabb.isInUse()) {
            return; // Already released
        }

        aabb.reset();
        totalReleased.incrementAndGet();

        if (top < capacity) {
            pool[top++] = aabb;
        }
    }

    /**
     * Retrieves an immutable telemetry report describing pool utilization.
     *
     * @return pool statistics snapshot
     */
    public synchronized VoxelAabbPoolStats getStats() {
        return new VoxelAabbPoolStats(
                capacity,
                top,
                totalAcquired.get(),
                totalReleased.get()
        );
    }

    public int getCapacity() {
        return capacity;
    }

    public synchronized int getAvailableCount() {
        return top;
    }

    public long getTotalAcquired() {
        return totalAcquired.get();
    }

    public long getTotalReleased() {
        return totalReleased.get();
    }
}
