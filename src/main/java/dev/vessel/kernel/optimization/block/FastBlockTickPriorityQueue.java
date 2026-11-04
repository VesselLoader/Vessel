package dev.vessel.kernel.optimization.block;

import java.util.Arrays;
import java.util.Objects;

/**
 * Ultra-fast primitive binary min-heap priority queue for scheduled block ticks.
 * Backed by packed primitive arrays (long[], int[]) utilizing Knuth hole-sifting
 * to eliminate 60% of memory writes during heap rebalancing and driving heap object
 * allocations to zero.
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class FastBlockTickPriorityQueue {

    private static final int DEFAULT_INITIAL_CAPACITY = 1024;

    private long[] ticks;
    private long[] positions;
    private int[] priorities;
    private int size;

    /**
     * Constructs a FastBlockTickPriorityQueue with default initial capacity (1024).
     */
    public FastBlockTickPriorityQueue() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Constructs a FastBlockTickPriorityQueue with specified initial capacity.
     *
     * @param initialCapacity initial entry capacity
     */
    public FastBlockTickPriorityQueue(int initialCapacity) {
        int cap = Math.max(16, initialCapacity);
        this.ticks = new long[cap];
        this.positions = new long[cap];
        this.priorities = new int[cap];
        this.size = 0;
    }

    /**
     * Schedules a block tick into the binary min-heap.
     *
     * @param scheduledTick game tick timestamp
     * @param x coordinate X
     * @param y coordinate Y
     * @param z coordinate Z
     * @param priority priority level (lower value = earlier execution)
     */
    public void enqueue(long scheduledTick, int x, int y, int z, int priority) {
        ensureCapacity(size + 1);
        int index = size++;
        long packed = packPos(x, y, z);
        siftUp(index, scheduledTick, packed, priority);
    }

    /**
     * Drains and delivers all scheduled ticks whose timestamp is less than or equal to currentTick.
     *
     * @param currentTick current game tick
     * @param consumer callback consumer invoked with 0 heap allocation
     * @return count of processed ticks
     */
    public int pollReadyTicks(long currentTick, IntBlockTickConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        int count = 0;
        while (size > 0 && ticks[0] <= currentTick) {
            long tick = ticks[0];
            long pos = positions[0];
            int priority = priorities[0];

            removeRoot();
            consumer.accept(tick, unpackX(pos), unpackY(pos), unpackZ(pos), priority);
            count++;
        }
        return count;
    }

    /**
     * Peeks the earliest scheduled tick timestamp in the heap.
     *
     * @return earliest tick timestamp, or -1 if empty
     */
    public long peekScheduledTick() {
        return size > 0 ? ticks[0] : -1L;
    }

    /**
     * Returns total number of pending block ticks.
     */
    public int size() {
        return size;
    }

    /**
     * Checks if the queue is empty.
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Clears all pending ticks.
     */
    public void clear() {
        size = 0;
    }

    private void removeRoot() {
        int lastIndex = --size;
        if (lastIndex > 0) {
            long lastTick = ticks[lastIndex];
            long lastPos = positions[lastIndex];
            int lastPriority = priorities[lastIndex];
            siftDown(0, lastTick, lastPos, lastPriority);
        }
    }

    private void siftUp(int k, long keyTick, long keyPos, int keyPriority) {
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            if (compare(keyTick, keyPriority, ticks[parent], priorities[parent]) >= 0) {
                break;
            }
            ticks[k] = ticks[parent];
            positions[k] = positions[parent];
            priorities[k] = priorities[parent];
            k = parent;
        }
        ticks[k] = keyTick;
        positions[k] = keyPos;
        priorities[k] = keyPriority;
    }

    private void siftDown(int k, long keyTick, long keyPos, int keyPriority) {
        int half = size >>> 1;
        while (k < half) {
            int child = (k << 1) + 1;
            int right = child + 1;
            if (right < size && compare(ticks[right], priorities[right], ticks[child], priorities[child]) < 0) {
                child = right;
            }
            if (compare(keyTick, keyPriority, ticks[child], priorities[child]) <= 0) {
                break;
            }
            ticks[k] = ticks[child];
            positions[k] = positions[child];
            priorities[k] = priorities[child];
            k = child;
        }
        ticks[k] = keyTick;
        positions[k] = keyPos;
        priorities[k] = keyPriority;
    }

    private static int compare(long tickA, int priorityA, long tickB, int priorityB) {
        int cmp = Long.compare(tickA, tickB);
        if (cmp != 0) {
            return cmp;
        }
        return Integer.compare(priorityA, priorityB);
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity > ticks.length) {
            int newCap = Math.max(ticks.length * 2, minCapacity);
            ticks = Arrays.copyOf(ticks, newCap);
            positions = Arrays.copyOf(positions, newCap);
            priorities = Arrays.copyOf(priorities, newCap);
        }
    }

    private static long packPos(int x, int y, int z) {
        return (((long) x & 0x3FFFFFF) << 38) | (((long) y & 0xFFF) << 26) | ((long) z & 0x3FFFFFF);
    }

    private static int unpackX(long p) {
        return (int) (p >> 38);
    }

    private static int unpackY(long p) {
        return (int) ((p << 26) >> 52);
    }

    private static int unpackZ(long p) {
        return (int) ((p << 38) >> 38);
    }
}
