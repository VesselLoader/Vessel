package dev.vessel.kernel.testmatrix;

import java.util.Arrays;

/**
 * Fixed-capacity circular ring buffer tracking micro-tick durations in nanoseconds without heap allocations.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class TickLatencyRingBuffer {

    private final long[] ring;
    private final int capacity;
    private int writeIndex = 0;
    private int count = 0;

    public TickLatencyRingBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        this.ring = new long[capacity];
    }

    /**
     * Records a tick duration sample in nanoseconds.
     *
     * @param durationNanos tick duration in nanoseconds
     */
    public synchronized void record(long durationNanos) {
        ring[writeIndex] = durationNanos;
        writeIndex = (writeIndex + 1) % capacity;
        if (count < capacity) {
            count++;
        }
    }

    public synchronized int getCount() {
        return count;
    }

    public int getCapacity() {
        return capacity;
    }

    /**
     * Calculates the arithmetic mean tick duration in nanoseconds.
     *
     * @return mean duration in nanoseconds
     */
    public synchronized double getMeanNanos() {
        if (count == 0) {
            return 0.0;
        }
        long sum = 0;
        for (int i = 0; i < count; i++) {
            sum += ring[i];
        }
        return (double) sum / count;
    }

    /**
     * Calculates the maximum observed tick duration in nanoseconds.
     *
     * @return maximum duration in nanoseconds
     */
    public synchronized long getMaxNanos() {
        if (count == 0) {
            return 0L;
        }
        long max = 0L;
        for (int i = 0; i < count; i++) {
            if (ring[i] > max) {
                max = ring[i];
            }
        }
        return max;
    }

    /**
     * Calculates the specified percentile latency in nanoseconds.
     *
     * @param percentile value between 0.0 and 100.0
     * @return percentile duration in nanoseconds
     */
    public synchronized double getPercentileNanos(double percentile) {
        if (count == 0) {
            return 0.0;
        }
        long[] copy = new long[count];
        System.arraycopy(ring, 0, copy, 0, count);
        Arrays.sort(copy);

        int index = (int) Math.ceil((percentile / 100.0) * count) - 1;
        if (index < 0) {
            index = 0;
        }
        if (index >= count) {
            index = count - 1;
        }
        return copy[index];
    }
}
