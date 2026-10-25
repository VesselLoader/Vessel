package dev.vessel.kernel.vfs;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * High-performance fixed-tier LIFO stack recycling direct {@link ByteBuffer} instances.
 * Operates without synchronization within a single-threaded ThreadLocal context.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class DirectBufferBucket {

    private final int capacityTier;
    private final int maxEntries;
    private final ByteBuffer[] stack;
    private int size;

    /**
     * Constructs a DirectBufferBucket for a specific capacity tier and slot limit.
     *
     * @param capacityTier fixed buffer capacity in bytes
     * @param maxEntries maximum number of buffers retained in this tier
     */
    public DirectBufferBucket(int capacityTier, int maxEntries) {
        if (capacityTier <= 0) {
            throw new IllegalArgumentException("capacityTier must be positive: " + capacityTier);
        }
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("maxEntries must be positive: " + maxEntries);
        }
        this.capacityTier = capacityTier;
        this.maxEntries = maxEntries;
        this.stack = new ByteBuffer[maxEntries];
        this.size = 0;
    }

    /**
     * Retrieves an idle pooled direct buffer if available.
     *
     * @return pooled ByteBuffer cleared for use, or null if bucket is empty
     */
    public ByteBuffer poll() {
        if (size == 0) {
            return null;
        }
        size--;
        ByteBuffer buffer = stack[size];
        stack[size] = null;
        buffer.clear();
        return buffer;
    }

    /**
     * Offers a released direct buffer back to this bucket.
     *
     * @param buffer direct buffer to recycle
     * @return true if accepted, false if bucket is full or incompatible
     */
    public boolean offer(ByteBuffer buffer) {
        Objects.requireNonNull(buffer, "buffer cannot be null");
        if (!buffer.isDirect() || buffer.capacity() != capacityTier || size >= maxEntries) {
            return false;
        }
        buffer.clear();
        stack[size++] = buffer;
        return true;
    }

    /**
     * Clears all held buffers from this bucket.
     */
    public void clear() {
        for (int i = 0; i < size; i++) {
            stack[i] = null;
        }
        size = 0;
    }

    /**
     * Returns the capacity tier in bytes.
     *
     * @return tier capacity
     */
    public int capacityTier() {
        return capacityTier;
    }

    /**
     * Returns current count of idle buffers held in this bucket.
     *
     * @return idle buffer count
     */
    public int size() {
        return size;
    }

    /**
     * Returns total off-heap memory retained by this bucket in bytes.
     *
     * @return retained memory in bytes
     */
    public long retainedBytes() {
        return (long) size * capacityTier;
    }
}
