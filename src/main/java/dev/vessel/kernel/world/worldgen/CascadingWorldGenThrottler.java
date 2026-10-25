package dev.vessel.kernel.world.worldgen;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Concurrency governor and deadlock eliminator for C2ME and Folia world generation.
 * <p>
 * Controls cross-thread cascading generation spikes when chunk decoration passes
 * access ungenerated adjacent neighbor chunks. Guarantees that recursive chunk loads
 * cannot exceed strict depth ceilings or cause inter-strand deadlocks.
 * </p>
 * <p>
 * Directly resolves ERR-33 and WGN-0001..0250.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class CascadingWorldGenThrottler {

    /**
     * Default maximum allowable cascading recursion depth before suppression.
     */
    public static final int DEFAULT_MAX_CASCADING_DEPTH = 2;

    /**
     * Default global maximum concurrent chunk generation operations.
     */
    public static final int DEFAULT_MAX_CONCURRENT_GENERATIONS = 32;

    private final int maxCascadingDepth;
    private final int maxConcurrentGenerations;
    private final Semaphore generationSemaphore;
    private final ConcurrentMap<Long, Long> activeGeneratingChunks;
    private final ConcurrentMap<Long, Long> threadWaitingOnChunk;
    private final AtomicLong totalCascadesThrottled;
    private final AtomicLong cyclesAvoided;

    /**
     * Constructs a throttler with default limits.
     */
    public CascadingWorldGenThrottler() {
        this(DEFAULT_MAX_CASCADING_DEPTH, DEFAULT_MAX_CONCURRENT_GENERATIONS);
    }

    /**
     * Constructs a throttler with custom depth and concurrency limits.
     *
     * @param maxCascadingDepth maximum allowed recursive decoration depth
     * @param maxConcurrentGenerations maximum concurrent threads actively generating chunks
     */
    public CascadingWorldGenThrottler(int maxCascadingDepth, int maxConcurrentGenerations) {
        if (maxCascadingDepth <= 0) {
            throw new IllegalArgumentException("maxCascadingDepth must be positive: " + maxCascadingDepth);
        }
        if (maxConcurrentGenerations <= 0) {
            throw new IllegalArgumentException("maxConcurrentGenerations must be positive: " + maxConcurrentGenerations);
        }
        this.maxCascadingDepth = maxCascadingDepth;
        this.maxConcurrentGenerations = maxConcurrentGenerations;
        this.generationSemaphore = new Semaphore(maxConcurrentGenerations, true);
        this.activeGeneratingChunks = new ConcurrentHashMap<>();
        this.threadWaitingOnChunk = new ConcurrentHashMap<>();
        this.totalCascadesThrottled = new AtomicLong(0L);
        this.cyclesAvoided = new AtomicLong(0L);
    }

    /**
     * Evaluates and attempts to acquire a generation permit for the specified chunk coordinate.
     *
     * @param chunkKey packed 64-bit chunk coordinate
     * @return generation permit detailing whether the thread may proceed
     */
    public GenerationPermit acquirePermit(long chunkKey) {
        long currentThreadId = Thread.currentThread().threadId();
        CascadingThreadDepthTracker tracker = CascadingThreadDepthTracker.current();
        int currentDepth = tracker.currentDepth();

        // Check 1: Circular generation on same thread
        if (tracker.contains(chunkKey)) {
            cyclesAvoided.incrementAndGet();
            return new GenerationPermit(chunkKey, currentThreadId, currentDepth, CascadingPermitStatus.REJECTED_CYCLE_DETECTED);
        }

        // Check 2: Cross-thread deadlock cycle detection
        Long holdingThreadId = activeGeneratingChunks.get(chunkKey);
        if (holdingThreadId != null && holdingThreadId != currentThreadId) {
            Long otherWaitingChunk = threadWaitingOnChunk.get(holdingThreadId);
            if (otherWaitingChunk != null && tracker.contains(otherWaitingChunk)) {
                cyclesAvoided.incrementAndGet();
                return new GenerationPermit(chunkKey, currentThreadId, currentDepth, CascadingPermitStatus.REJECTED_CYCLE_DETECTED);
            }
        }

        // Check 3: Cascading recursion depth limit
        if (currentDepth >= maxCascadingDepth) {
            totalCascadesThrottled.incrementAndGet();
            return new GenerationPermit(chunkKey, currentThreadId, currentDepth, CascadingPermitStatus.REJECTED_DEPTH_EXCEEDED);
        }

        // Check 4: Concurrency capacity limit
        boolean acquired = generationSemaphore.tryAcquire();
        if (!acquired) {
            return new GenerationPermit(chunkKey, currentThreadId, currentDepth, CascadingPermitStatus.REJECTED_CAPACITY_EXCEEDED);
        }

        activeGeneratingChunks.put(chunkKey, currentThreadId);
        tracker.push(chunkKey);
        return new GenerationPermit(chunkKey, currentThreadId, currentDepth, CascadingPermitStatus.ACQUIRED);
    }

    /**
     * Safely releases a previously acquired generation permit.
     *
     * @param permit non-null permit previously returned by {@link #acquirePermit(long)}
     */
    public void releasePermit(GenerationPermit permit) {
        Objects.requireNonNull(permit, "permit cannot be null");

        if (permit.status() != CascadingPermitStatus.ACQUIRED) {
            return;
        }

        activeGeneratingChunks.remove(permit.chunkKey());
        threadWaitingOnChunk.remove(permit.threadId());

        CascadingThreadDepthTracker tracker = CascadingThreadDepthTracker.current();
        if (tracker.currentDepth() > 0 && tracker.peek() == permit.chunkKey()) {
            tracker.pop();
        }

        generationSemaphore.release();
    }

    /**
     * Checks if a given chunk is currently actively undergoing generation.
     *
     * @param chunkKey packed 64-bit chunk coordinate
     * @return {@code true} if generating
     */
    public boolean isCurrentlyGenerating(long chunkKey) {
        return activeGeneratingChunks.containsKey(chunkKey);
    }

    /**
     * Returns the count of chunks currently undergoing generation across all threads.
     *
     * @return active generation count
     */
    public int activeGenerationsCount() {
        return activeGeneratingChunks.size();
    }

    /**
     * Returns the lifetime count of cascading generation cascades suppressed.
     *
     * @return throttled cascades count
     */
    public long totalCascadesThrottled() {
        return totalCascadesThrottled.get();
    }

    /**
     * Returns the lifetime count of circular generation cycles avoided.
     *
     * @return cycles avoided count
     */
    public long cyclesAvoided() {
        return cyclesAvoided.get();
    }

    /**
     * Returns the configured maximum cascading depth.
     *
     * @return max cascading depth
     */
    public int maxCascadingDepth() {
        return maxCascadingDepth;
    }

    /**
     * Returns the configured maximum concurrent generations.
     *
     * @return max concurrent generations
     */
    public int maxConcurrentGenerations() {
        return maxConcurrentGenerations;
    }
}
