package dev.vessel.kernel.optimization.physics;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * High-performance object recycling pool for Axis-Aligned Bounding Boxes (AABB).
 * <p>
 * Replaces high-frequency heap allocations of collision boxes in physics, block raytracing, and entity
 * movement collision loops. Utilizes thread-local non-blocking recycling stacks ({@link LocalVoxelAabbStack})
 * to drive thread contention down to zero and per-operation heap allocations down to zero bytes.
 * </p>
 *
 * @version 1.0.2
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

    private final ThreadLocal<LocalVoxelAabbStack> localStacks;

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
        this.localStacks = ThreadLocal.withInitial(() -> new LocalVoxelAabbStack(this));
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
     * Acquires a recycled {@link PooledVoxelAabb} instance using lock-free thread-local recycling.
     *
     * @param minX minimum X boundary
     * @param minY minimum Y boundary
     * @param minZ minimum Z boundary
     * @param maxX maximum X boundary
     * @param maxY maximum Y boundary
     * @param maxZ maximum Z boundary
     * @return initialized pooled bounding box
     */
    public PooledVoxelAabb acquire(
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ
    ) {
        return localStacks.get().acquire(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Releases an active {@link PooledVoxelAabb} back into the thread-local recycling stack.
     *
     * @param aabb bounding box to recycle
     */
    public void release(PooledVoxelAabb aabb) {
        localStacks.get().release(aabb);
    }

    /**
     * Acquires directly from the synchronized global pool, used when thread-local stacks are exhausted.
     *
     * @param minX minimum X boundary
     * @param minY minimum Y boundary
     * @param minZ minimum Z boundary
     * @param maxX maximum X boundary
     * @param maxY maximum Y boundary
     * @param maxZ maximum Z boundary
     * @return initialized pooled bounding box
     */
    public synchronized PooledVoxelAabb acquireDirect(
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
     * Releases an active {@link PooledVoxelAabb} directly back into the synchronized global pool.
     *
     * @param aabb bounding box to recycle
     */
    public synchronized void releaseDirect(PooledVoxelAabb aabb) {
        Objects.requireNonNull(aabb, "aabb cannot be null");
        if (!aabb.isInUse()) {
            return;
        }

        aabb.reset();
        totalReleased.incrementAndGet();

        if (top < capacity) {
            pool[top++] = aabb;
        }
    }

    synchronized void releaseDirectUncounted(PooledVoxelAabb aabb) {
        if (top < capacity) {
            pool[top++] = aabb;
        }
    }

    void recordAcquired() {
        totalAcquired.incrementAndGet();
    }

    void recordReleased() {
        totalReleased.incrementAndGet();
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
        return top + localStacks.get().getAvailableCount();
    }

    public long getTotalAcquired() {
        return totalAcquired.get();
    }

    public long getTotalReleased() {
        return totalReleased.get();
    }
}
