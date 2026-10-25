package dev.vessel.kernel.vfs;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-confined direct off-heap {@link ByteBuffer} recycling coordinator.
 * Completely eliminates off-heap native memory exhaustion and GC pauses caused by continuous
 * allocation of native buffers in I/O and virtual file system reading routines (OPT-020..OPT-035, ERR-20).
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ThreadLocalDirectBufferPoolRecycler {

    private static final int[] DEFAULT_TIERS = new int[] {
            4 * 1024,      // 4 KB
            16 * 1024,     // 16 KB
            64 * 1024,     // 64 KB
            256 * 1024,    // 256 KB
            1024 * 1024    // 1 MB
    };

    private static final int DEFAULT_MAX_ENTRIES_PER_TIER = 4;
    private static final long DEFAULT_MAX_RETAINED_BYTES_PER_THREAD = 16L * 1024 * 1024; // 16 MB

    private static final ThreadLocal<ThreadLocalBufferHolder> HOLDER =
            ThreadLocal.withInitial(() -> new ThreadLocalBufferHolder(
                    DEFAULT_TIERS,
                    DEFAULT_MAX_ENTRIES_PER_TIER,
                    DEFAULT_MAX_RETAINED_BYTES_PER_THREAD
            ));

    private static final AtomicLong TOTAL_ACQUIRED = new AtomicLong(0);
    private static final AtomicLong TOTAL_RECYCLED = new AtomicLong(0);

    private ThreadLocalDirectBufferPoolRecycler() {
    }

    /**
     * Acquires a pooled or newly allocated direct {@link ByteBuffer} with at least the requested capacity.
     * The returned buffer is cleared (position=0, limit=capacity).
     *
     * @param minCapacity minimum required capacity in bytes
     * @return direct ByteBuffer ready for I/O operations
     */
    public static ByteBuffer acquire(int minCapacity) {
        TOTAL_ACQUIRED.incrementAndGet();
        return HOLDER.get().acquire(minCapacity);
    }

    /**
     * Recycles a previously acquired direct {@link ByteBuffer} back into the thread pool.
     *
     * @param buffer direct buffer to recycle
     */
    public static void release(ByteBuffer buffer) {
        if (buffer != null && buffer.isDirect()) {
            HOLDER.get().release(buffer);
            TOTAL_RECYCLED.incrementAndGet();
        }
    }

    /**
     * Clears all pooled direct buffers associated with the calling thread.
     * Crucial to call upon worker thread termination or when resetting thread pool contexts.
     */
    public static void clearCurrentThread() {
        HOLDER.get().clear();
    }

    /**
     * Removes the ThreadLocal entry entirely for the calling thread.
     */
    public static void removeCurrentThread() {
        HOLDER.get().clear();
        HOLDER.remove();
    }

    /**
     * Returns the total amount of off-heap bytes currently retained by the calling thread's pools.
     *
     * @return retained memory in bytes
     */
    public static long currentThreadRetainedBytes() {
        return HOLDER.get().currentRetainedBytes();
    }

    /**
     * Returns the global cumulative count of buffer acquisitions.
     *
     * @return total acquire calls
     */
    public static long totalAcquiredCount() {
        return TOTAL_ACQUIRED.get();
    }

    /**
     * Returns the global cumulative count of buffer releases.
     *
     * @return total release calls
     */
    public static long totalRecycledCount() {
        return TOTAL_RECYCLED.get();
    }
}
