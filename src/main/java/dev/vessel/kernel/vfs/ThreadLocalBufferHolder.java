package dev.vessel.kernel.vfs;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Thread-confined container managing multiple capacity-tiered {@link DirectBufferBucket} pools.
 * Enforces per-thread memory ceilings to prevent native memory bloat across worker threads (ERR-20).
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ThreadLocalBufferHolder {

    private final DirectBufferBucket[] buckets;
    private final long maxRetainedBytes;
    private long currentRetainedBytes = 0;

    /**
     * Constructs a ThreadLocalBufferHolder with specified capacity tiers, entries per tier, and byte limit.
     *
     * @param tiers array of byte capacities for tiers (must be ascending)
     * @param maxEntriesPerTier maximum entries per tier
     * @param maxRetainedBytes maximum total bytes retained across all tiers in this thread
     */
    public ThreadLocalBufferHolder(int[] tiers, int maxEntriesPerTier, long maxRetainedBytes) {
        Objects.requireNonNull(tiers, "tiers cannot be null");
        this.maxRetainedBytes = maxRetainedBytes;
        this.buckets = new DirectBufferBucket[tiers.length];
        for (int i = 0; i < tiers.length; i++) {
            this.buckets[i] = new DirectBufferBucket(tiers[i], maxEntriesPerTier);
        }
    }

    /**
     * Acquires a direct ByteBuffer with at least the requested capacity.
     *
     * @param minCapacity minimum required capacity in bytes
     * @return direct ByteBuffer ready for use
     */
    public ByteBuffer acquire(int minCapacity) {
        if (minCapacity <= 0) {
            minCapacity = 1024;
        }

        // Find smallest tier >= minCapacity
        for (int i = 0; i < buckets.length; i++) {
            if (buckets[i].capacityTier() >= minCapacity) {
                ByteBuffer pooled = buckets[i].poll();
                if (pooled != null) {
                    currentRetainedBytes -= buckets[i].capacityTier();
                    return pooled;
                }
                return ByteBuffer.allocateDirect(buckets[i].capacityTier());
            }
        }

        // Exceeds largest tier: allocate unpooled direct buffer
        return ByteBuffer.allocateDirect(minCapacity);
    }

    /**
     * Recycles a direct ByteBuffer back into the appropriate tier if within memory limits.
     *
     * @param buffer direct buffer to recycle
     */
    public void release(ByteBuffer buffer) {
        if (buffer == null || !buffer.isDirect()) {
            return;
        }

        int capacity = buffer.capacity();
        if (currentRetainedBytes + capacity > maxRetainedBytes) {
            // Memory ceiling exceeded; drop buffer to allow JVM cleaner reclamation
            return;
        }

        for (int i = 0; i < buckets.length; i++) {
            if (buckets[i].capacityTier() == capacity) {
                if (buckets[i].offer(buffer)) {
                    currentRetainedBytes += capacity;
                }
                return;
            }
        }
    }

    /**
     * Clears all pooled buffers in this thread-local holder.
     */
    public void clear() {
        for (int i = 0; i < buckets.length; i++) {
            buckets[i].clear();
        }
        currentRetainedBytes = 0;
    }

    /**
     * Returns total off-heap bytes currently retained by this thread's pools.
     *
     * @return retained memory in bytes
     */
    public long currentRetainedBytes() {
        return currentRetainedBytes;
    }
}
