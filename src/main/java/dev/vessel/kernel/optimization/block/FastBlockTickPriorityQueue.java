package dev.vessel.kernel.optimization.block;

import java.util.Arrays;
import java.util.Objects;

/**
 * Ultra-fast primitive binary min-heap priority queue for scheduled block ticks.
 * Backed entirely by flat parallel primitive arrays (long[], int[]), completely eliminating
 * heap object allocation and GC pressure even when tens of thousands of block ticks are scheduled.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class FastBlockTickPriorityQueue {

    private static final int DEFAULT_INITIAL_CAPACITY = 1024;

    private long[] ticks;
    private int[] xs;
    private int[] ys;
    private int[] zs;
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
        this.xs = new int[cap];
        this.ys = new int[cap];
        this.zs = new int[cap];
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
        ticks[index] = scheduledTick;
        xs[index] = x;
        ys[index] = y;
        zs[index] = z;
        priorities[index] = priority;
        siftUp(index);
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
            int x = xs[0];
            int y = ys[0];
            int z = zs[0];
            int priority = priorities[0];

            removeRoot();
            consumer.accept(tick, x, y, z, priority);
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
            ticks[0] = ticks[lastIndex];
            xs[0] = xs[lastIndex];
            ys[0] = ys[lastIndex];
            zs[0] = zs[lastIndex];
            priorities[0] = priorities[lastIndex];
            siftDown(0);
        }
    }

    private void siftUp(int k) {
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            if (compare(k, parent) >= 0) {
                break;
            }
            swap(k, parent);
            k = parent;
        }
    }

    private void siftDown(int k) {
        int half = size >>> 1;
        while (k < half) {
            int child = (k << 1) + 1;
            int right = child + 1;
            if (right < size && compare(right, child) < 0) {
                child = right;
            }
            if (compare(k, child) <= 0) {
                break;
            }
            swap(k, child);
            k = child;
        }
    }

    private int compare(int i, int j) {
        int cmp = Long.compare(ticks[i], ticks[j]);
        if (cmp != 0) {
            return cmp;
        }
        return Integer.compare(priorities[i], priorities[j]);
    }

    private void swap(int i, int j) {
        long t = ticks[i]; ticks[i] = ticks[j]; ticks[j] = t;
        int x = xs[i]; xs[i] = xs[j]; xs[j] = x;
        int y = ys[i]; ys[i] = ys[j]; ys[j] = y;
        int z = zs[i]; zs[i] = zs[j]; zs[j] = z;
        int p = priorities[i]; priorities[i] = priorities[j]; priorities[j] = p;
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity > ticks.length) {
            int newCap = Math.max(ticks.length * 2, minCapacity);
            ticks = Arrays.copyOf(ticks, newCap);
            xs = Arrays.copyOf(xs, newCap);
            ys = Arrays.copyOf(ys, newCap);
            zs = Arrays.copyOf(zs, newCap);
            priorities = Arrays.copyOf(priorities, newCap);
        }
    }
}
