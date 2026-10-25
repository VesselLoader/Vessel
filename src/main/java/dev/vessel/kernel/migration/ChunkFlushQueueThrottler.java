package dev.vessel.kernel.migration;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Throttler and spatial deduplicator for pending chunk flush tasks.
 * <p>
 * When disk write bandwidth becomes a bottleneck, deduplicates superseded chunk snapshots
 * targeting identical (dimension, chunkX, chunkZ) spatial coordinates, ensuring only the freshest
 * world state is committed to storage.
 * </p>
 * <p>
 * Mitigates: <b>ERR-98</b>, <b>ERR-100</b>, <b>STO-051..100</b>.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ChunkFlushQueueThrottler {

    /** Default soft throttling threshold queue size. */
    public static final int DEFAULT_THROTTLE_THRESHOLD = 2048;

    /** Default hard maximum queue capacity. */
    public static final int DEFAULT_MAX_CAPACITY = 4096;

    private final int throttleThreshold;
    private final int maxCapacity;
    private final ConcurrentMap<String, Long> pendingChunkTicks;

    /**
     * Constructs a throttler with default capacity settings.
     */
    public ChunkFlushQueueThrottler() {
        this(DEFAULT_THROTTLE_THRESHOLD, DEFAULT_MAX_CAPACITY);
    }

    /**
     * Constructs a throttler with custom thresholds.
     *
     * @param throttleThreshold queue size at which aggressive deduplication begins
     * @param maxCapacity       maximum allowed queue capacity
     */
    public ChunkFlushQueueThrottler(int throttleThreshold, int maxCapacity) {
        if (throttleThreshold <= 0 || maxCapacity <= throttleThreshold) {
            throw new IllegalArgumentException("Invalid queue thresholds: threshold=" + throttleThreshold + ", max=" + maxCapacity);
        }
        this.throttleThreshold = throttleThreshold;
        this.maxCapacity = maxCapacity;
        this.pendingChunkTicks = new ConcurrentHashMap<>();
    }

    /**
     * Evaluates whether an incoming chunk snapshot supersedes a currently pending flush task.
     *
     * @param snapshot the snapshot to inspect
     * @return true if an older snapshot for this coordinate was superseded
     */
    public boolean registerSnapshot(ImmutableChunkSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot cannot be null");
        String key = snapshot.chunkKey();
        Long previousTick = pendingChunkTicks.put(key, snapshot.worldTick());
        return previousTick != null && snapshot.worldTick() >= previousTick;
    }

    /**
     * Deregisters a chunk coordinate upon successful disk write completion.
     *
     * @param snapshot the written snapshot
     */
    public void deregisterSnapshot(ImmutableChunkSnapshot snapshot) {
        if (snapshot != null) {
            pendingChunkTicks.remove(snapshot.chunkKey(), snapshot.worldTick());
        }
    }

    /**
     * Checks if the queue has reached the hard maximum capacity.
     *
     * @param currentQueueSize current pending tasks count
     * @return true if the queue is saturated
     */
    public boolean isSaturated(int currentQueueSize) {
        return currentQueueSize >= maxCapacity;
    }

    /**
     * Checks if the queue has crossed the soft throttling threshold.
     *
     * @param currentQueueSize current pending tasks count
     * @return true if throttling should activate
     */
    public boolean shouldThrottle(int currentQueueSize) {
        return currentQueueSize >= throttleThreshold;
    }

    /**
     * Returns the configured throttle threshold.
     *
     * @return throttle threshold count
     */
    public int throttleThreshold() {
        return throttleThreshold;
    }

    /**
     * Returns the configured max capacity.
     *
     * @return max capacity count
     */
    public int maxCapacity() {
        return maxCapacity;
    }
}
