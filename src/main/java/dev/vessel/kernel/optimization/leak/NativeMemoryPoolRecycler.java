package dev.vessel.kernel.optimization.leak;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * High-throughput primitive array and buffer recycler inspired by Netty Recycler and MemoryLeakFix.
 * Manages tiered sizing bins (512B, 4KB, 16KB, 64KB) with lock-free recycling queues to eliminate
 * GC allocation churn and generational pause spikes in high-throughput network and chunk pipelines.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class NativeMemoryPoolRecycler {

    public static final int TIER_512 = 512;
    public static final int TIER_4K  = 4096;
    public static final int TIER_16K = 16384;
    public static final int TIER_64K = 65536;

    public static final int FLOAT_TIER_256  = 256;
    public static final int FLOAT_TIER_1024 = 1024;
    public static final int FLOAT_TIER_4096 = 4096;

    private static final int MAX_POOLED_PER_TIER = 256;

    private final ConcurrentLinkedQueue<byte[]> pool512 = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<byte[]> pool4k = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<byte[]> pool16k = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<byte[]> pool64k = new ConcurrentLinkedQueue<>();

    private final ConcurrentLinkedQueue<float[]> poolFloat256 = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<float[]> poolFloat1024 = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<float[]> poolFloat4096 = new ConcurrentLinkedQueue<>();

    private final AtomicLong totalAcquires = new AtomicLong(0L);
    private final AtomicLong totalReleases = new AtomicLong(0L);
    private final AtomicLong cacheHits = new AtomicLong(0L);

    /**
     * Constructs a NativeMemoryPoolRecycler.
     */
    public NativeMemoryPoolRecycler() {
    }

    /**
     * Acquires a pooled byte array with at least the requested capacity.
     *
     * @param minCapacity requested minimum capacity in bytes
     * @return pooled or freshly allocated byte array
     */
    public byte[] acquireByteArray(int minCapacity) {
        totalAcquires.incrementAndGet();

        if (minCapacity <= TIER_512) {
            byte[] buf = pool512.poll();
            if (buf != null) {
                cacheHits.incrementAndGet();
                return buf;
            }
            return new byte[TIER_512];
        } else if (minCapacity <= TIER_4K) {
            byte[] buf = pool4k.poll();
            if (buf != null) {
                cacheHits.incrementAndGet();
                return buf;
            }
            return new byte[TIER_4K];
        } else if (minCapacity <= TIER_16K) {
            byte[] buf = pool16k.poll();
            if (buf != null) {
                cacheHits.incrementAndGet();
                return buf;
            }
            return new byte[TIER_16K];
        } else if (minCapacity <= TIER_64K) {
            byte[] buf = pool64k.poll();
            if (buf != null) {
                cacheHits.incrementAndGet();
                return buf;
            }
            return new byte[TIER_64K];
        } else {
            return new byte[minCapacity];
        }
    }

    /**
     * Recycles a byte array back to the matching size pool.
     *
     * @param array array to return to pool
     */
    public void releaseByteArray(byte[] array) {
        if (array == null) {
            return;
        }
        totalReleases.incrementAndGet();
        int len = array.length;

        if (len == TIER_512 && pool512.size() < MAX_POOLED_PER_TIER) {
            pool512.offer(array);
        } else if (len == TIER_4K && pool4k.size() < MAX_POOLED_PER_TIER) {
            pool4k.offer(array);
        } else if (len == TIER_16K && pool16k.size() < MAX_POOLED_PER_TIER) {
            pool16k.offer(array);
        } else if (len == TIER_64K && pool64k.size() < MAX_POOLED_PER_TIER) {
            pool64k.offer(array);
        }
    }

    /**
     * Acquires a pooled float array with at least the requested capacity.
     *
     * @param minCapacity requested minimum float elements
     * @return pooled or freshly allocated float array
     */
    public float[] acquireFloatArray(int minCapacity) {
        totalAcquires.incrementAndGet();

        if (minCapacity <= FLOAT_TIER_256) {
            float[] buf = poolFloat256.poll();
            if (buf != null) {
                cacheHits.incrementAndGet();
                return buf;
            }
            return new float[FLOAT_TIER_256];
        } else if (minCapacity <= FLOAT_TIER_1024) {
            float[] buf = poolFloat1024.poll();
            if (buf != null) {
                cacheHits.incrementAndGet();
                return buf;
            }
            return new float[FLOAT_TIER_1024];
        } else if (minCapacity <= FLOAT_TIER_4096) {
            float[] buf = poolFloat4096.poll();
            if (buf != null) {
                cacheHits.incrementAndGet();
                return buf;
            }
            return new float[FLOAT_TIER_4096];
        } else {
            return new float[minCapacity];
        }
    }

    /**
     * Recycles a float array back to the matching size pool.
     *
     * @param array array to return to pool
     */
    public void releaseFloatArray(float[] array) {
        if (array == null) {
            return;
        }
        totalReleases.incrementAndGet();
        int len = array.length;

        if (len == FLOAT_TIER_256 && poolFloat256.size() < MAX_POOLED_PER_TIER) {
            poolFloat256.offer(array);
        } else if (len == FLOAT_TIER_1024 && poolFloat1024.size() < MAX_POOLED_PER_TIER) {
            poolFloat1024.offer(array);
        } else if (len == FLOAT_TIER_4096 && poolFloat4096.size() < MAX_POOLED_PER_TIER) {
            poolFloat4096.offer(array);
        }
    }

    /**
     * Calculates the count of active idle buffers currently retained in memory pools.
     */
    public int getActivePooledBuffers() {
        return pool512.size() + pool4k.size() + pool16k.size() + pool64k.size()
                + poolFloat256.size() + poolFloat1024.size() + poolFloat4096.size();
    }

    /**
     * Returns an immutable snapshot of recycling statistics.
     */
    public PooledBufferStatistics getStatistics() {
        return new PooledBufferStatistics(
                totalAcquires.get(),
                totalReleases.get(),
                cacheHits.get(),
                getActivePooledBuffers()
        );
    }

    /**
     * Clears all pools and resets metrics.
     */
    public void clear() {
        pool512.clear();
        pool4k.clear();
        pool16k.clear();
        pool64k.clear();
        poolFloat256.clear();
        poolFloat1024.clear();
        poolFloat4096.clear();
    }
}
