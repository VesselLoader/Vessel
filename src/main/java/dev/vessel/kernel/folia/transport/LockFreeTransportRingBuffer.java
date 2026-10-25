package dev.vessel.kernel.folia.transport;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Consumer;

/**
 * High-performance, lock-free Multi-Producer Single-Consumer (MPSC) ring buffer
 * designed for cross-region resource transport in Folia thread strands.
 * <p>
 * Enables arbitrary concurrent producer threads (e.g. adjacent region threads or cable grids)
 * to enqueue machine energy, fluid, and item packets without taking locks or causing thread deadlocks.
 * The owning single consumer region thread drains the buffer sequentially during its local tick.
 * </p>
 * <p>
 * Directly resolves ERR-14, ERR-18, and FOL-001..020.
 * </p>
 *
 * @param <T> the resource packet or transport task type
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class LockFreeTransportRingBuffer<T> {

    /**
     * Default capacity for standard chunk transport buffers (4096 elements).
     */
    public static final int DEFAULT_CAPACITY = 4096;

    /**
     * Minimum allowable capacity for the ring buffer.
     */
    public static final int MIN_CAPACITY = 16;

    /**
     * Maximum allowable capacity for the ring buffer (1,048,576 elements).
     */
    public static final int MAX_CAPACITY = 1 << 20;

    private final int capacity;
    private final int mask;
    private final AtomicReferenceArray<T> buffer;
    private final AtomicLong tail;
    private final AtomicLong head;

    /**
     * Creates a ring buffer with the default capacity of {@value #DEFAULT_CAPACITY}.
     */
    public LockFreeTransportRingBuffer() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Creates a ring buffer with the specified capacity rounded up to the nearest power of two.
     *
     * @param requestedCapacity desired minimum buffer capacity
     * @throws IllegalArgumentException if requestedCapacity is outside valid bounds
     */
    public LockFreeTransportRingBuffer(int requestedCapacity) {
        if (requestedCapacity < MIN_CAPACITY || requestedCapacity > MAX_CAPACITY) {
            throw new IllegalArgumentException("Capacity must be between " + MIN_CAPACITY + " and " + MAX_CAPACITY);
        }
        this.capacity = calculateNextPowerOfTwo(requestedCapacity);
        this.mask = this.capacity - 1;
        this.buffer = new AtomicReferenceArray<>(this.capacity);
        this.tail = new AtomicLong(0L);
        this.head = new AtomicLong(0L);
    }

    /**
     * Attempts to enqueue a transport item in a non-blocking, lock-free manner.
     * Thread-safe for any number of concurrent producer threads.
     *
     * @param item non-null item to enqueue
     * @return {@code true} if enqueued successfully; {@code false} if the buffer is full
     * @throws NullPointerException if item is null
     */
    public boolean offer(T item) {
        Objects.requireNonNull(item, "item cannot be null");

        while (true) {
            long currentTail = tail.get();
            long currentHead = head.get();

            if (currentTail - currentHead >= capacity) {
                return false;
            }

            if (tail.compareAndSet(currentTail, currentTail + 1)) {
                int slot = (int) (currentTail & mask);
                while (!buffer.compareAndSet(slot, null, item)) {
                    Thread.onSpinWait();
                }
                return true;
            }
        }
    }

    /**
     * Polls the next item from the ring buffer.
     * Only the designated single consumer thread should invoke this method.
     *
     * @return the next item, or {@code null} if empty or unpublished
     */
    public T poll() {
        long currentHead = head.get();
        long currentTail = tail.get();

        if (currentHead >= currentTail) {
            return null;
        }

        int slot = (int) (currentHead & mask);
        T item = buffer.get(slot);
        if (item == null) {
            return null;
        }

        buffer.set(slot, null);
        head.lazySet(currentHead + 1);
        return item;
    }

    /**
     * Drains all currently available items into the given consumer callback.
     * Only the designated single consumer thread should invoke this method.
     *
     * @param action non-null consumer action
     * @return the number of items drained
     * @throws NullPointerException if action is null
     */
    public int drainTo(Consumer<T> action) {
        Objects.requireNonNull(action, "action cannot be null");
        return drainBatch(action, capacity);
    }

    /**
     * Drains up to {@code maxBatch} items into the given consumer callback.
     *
     * @param action non-null consumer action
     * @param maxBatch maximum number of items to process
     * @return number of items drained
     * @throws NullPointerException if action is null
     */
    public int drainBatch(Consumer<T> action, int maxBatch) {
        Objects.requireNonNull(action, "action cannot be null");
        if (maxBatch <= 0) {
            return 0;
        }

        int count = 0;
        while (count < maxBatch) {
            T item = poll();
            if (item == null) {
                break;
            }
            action.accept(item);
            count++;
        }
        return count;
    }

    /**
     * Returns the configured capacity of this ring buffer.
     *
     * @return buffer capacity
     */
    public int capacity() {
        return capacity;
    }

    /**
     * Returns an estimate of the current number of elements in the buffer.
     *
     * @return approximate element count
     */
    public int size() {
        long diff = tail.get() - head.get();
        if (diff < 0) {
            return 0;
        }
        if (diff > capacity) {
            return capacity;
        }
        return (int) diff;
    }

    /**
     * Checks if the buffer is currently empty.
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        return head.get() >= tail.get();
    }

    /**
     * Checks if the buffer is currently full.
     *
     * @return {@code true} if full
     */
    public boolean isFull() {
        return (tail.get() - head.get()) >= capacity;
    }

    /**
     * Clears all items currently in the buffer.
     */
    public void clear() {
        while (poll() != null) {
            // Drain all remaining
        }
    }

    private static int calculateNextPowerOfTwo(int value) {
        int highestOneBit = Integer.highestOneBit(value);
        if (value == highestOneBit) {
            return value;
        }
        return highestOneBit << 1;
    }
}
