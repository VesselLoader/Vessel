package dev.vessel.kernel.optimization.physics;

import java.util.Objects;

/**
 * Thread-local non-blocking recycling stack for {@link PooledVoxelAabb} instances.
 * <p>
 * Eliminates synchronization and CPU cache-line thrashing by providing each execution thread
 * with an isolated, lock-free LIFO pool. Out-of-capacity acquisitions and spills seamlessly
 * cascade to and from the shared {@link VoxelShapeAabbCachePool}.
 * </p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class LocalVoxelAabbStack {

    public static final int DEFAULT_LOCAL_CAPACITY = 256;

    private final VoxelShapeAabbCachePool globalPool;
    private final PooledVoxelAabb[] elements;
    private final int capacity;
    private int top;

    /**
     * Constructs a thread-local stack backed by the specified global pool with default capacity.
     *
     * @param globalPool the parent global pool
     */
    public LocalVoxelAabbStack(VoxelShapeAabbCachePool globalPool) {
        this(globalPool, DEFAULT_LOCAL_CAPACITY);
    }

    /**
     * Constructs a thread-local stack backed by the specified global pool with explicit capacity.
     *
     * @param globalPool the parent global pool
     * @param capacity   maximum cached bounding boxes in this thread's local stack
     */
    public LocalVoxelAabbStack(VoxelShapeAabbCachePool globalPool, int capacity) {
        this.globalPool = Objects.requireNonNull(globalPool, "globalPool cannot be null");
        this.capacity = Math.max(16, capacity);
        this.elements = new PooledVoxelAabb[this.capacity];
        this.top = 0;
    }

    /**
     * Acquires a recycled {@link PooledVoxelAabb} from this thread's local stack, or delegates
     * to the global pool if the local stack is empty.
     *
     * @param minX minimum X boundary
     * @param minY minimum Y boundary
     * @param minZ minimum Z boundary
     * @param maxX maximum X boundary
     * @param maxY maximum Y boundary
     * @param maxZ maximum Z boundary
     * @return initialized bounding box
     */
    public PooledVoxelAabb acquire(
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ
    ) {
        PooledVoxelAabb instance;
        if (top > 0) {
            instance = elements[--top];
            instance.set(minX, minY, minZ, maxX, maxY, maxZ);
            globalPool.recordAcquired();
        } else {
            instance = globalPool.acquireDirect(minX, minY, minZ, maxX, maxY, maxZ);
        }
        return instance;
    }

    /**
     * Recycles an active {@link PooledVoxelAabb} back into this thread's local stack, or spills
     * to the parent global pool if this local stack is saturated.
     *
     * @param aabb bounding box to recycle
     */
    public void release(PooledVoxelAabb aabb) {
        if (aabb == null || !aabb.isInUse()) {
            return;
        }
        aabb.reset();
        globalPool.recordReleased();
        if (top < capacity) {
            elements[top++] = aabb;
        } else {
            globalPool.releaseDirectUncounted(aabb);
        }
    }

    /**
     * Returns the number of bounding boxes currently ready in this thread's local stack.
     *
     * @return local available count
     */
    public int getAvailableCount() {
        return top;
    }

    /**
     * Returns the maximum capacity of this thread-local stack.
     *
     * @return local capacity
     */
    public int getCapacity() {
        return capacity;
    }
}
