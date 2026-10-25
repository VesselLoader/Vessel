package dev.vessel.kernel.tick;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Asynchronous Actor Mailbox coordinating cross-region task dispatch in Folia.
 * <p>
 * Implements the Actor Ask Pattern with strict execution deadlines to prevent
 * cascading stalls and deadlocks across adjacent 16&times;16 chunk regions.
 * Tasks are drained and processed exclusively during the owning strand's tick pass.
 * </p>
 * <p>
 * Directly resolves ERR-31, ERR-32, and FOL-041..060.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class CrossRegionActorMailbox {

    /**
     * Default maximum pending tasks permitted in this mailbox before shedding load.
     */
    public static final int DEFAULT_MAX_QUEUE_CAPACITY = 16384;

    /**
     * Default timeout deadline in milliseconds for cross-region ask requests (5 game ticks = 250ms).
     */
    public static final long DEFAULT_ASK_TIMEOUT_MILLIS = 250L;

    private final long regionKey;
    private final int maxQueueCapacity;
    private final ConcurrentLinkedQueue<CrossRegionEnvelope> queue;
    private final AtomicInteger queueSize;
    private final AtomicLong nextRequestId;
    private final AtomicLong processedCount;
    private final AtomicLong timeoutCount;
    private final AtomicLong rejectedCount;

    /**
     * Constructs a mailbox for the given Folia region key with default capacity.
     *
     * @param regionKey packed 64-bit coordinate key of this region strand
     */
    public CrossRegionActorMailbox(long regionKey) {
        this(regionKey, DEFAULT_MAX_QUEUE_CAPACITY);
    }

    /**
     * Constructs a mailbox for the given Folia region key with specified maximum capacity.
     *
     * @param regionKey packed 64-bit coordinate key of this region strand
     * @param maxQueueCapacity maximum allowed queue depth
     */
    public CrossRegionActorMailbox(long regionKey, int maxQueueCapacity) {
        if (maxQueueCapacity <= 0) {
            throw new IllegalArgumentException("maxQueueCapacity must be positive: " + maxQueueCapacity);
        }
        this.regionKey = regionKey;
        this.maxQueueCapacity = maxQueueCapacity;
        this.queue = new ConcurrentLinkedQueue<>();
        this.queueSize = new AtomicInteger(0);
        this.nextRequestId = new AtomicLong(1L);
        this.processedCount = new AtomicLong(0L);
        this.timeoutCount = new AtomicLong(0L);
        this.rejectedCount = new AtomicLong(0L);
    }

    /**
     * Asynchronously delivers a fire-and-forget task (Actor Tell pattern) to this region.
     *
     * @param sourceRegionKey packed region key of the originating strand
     * @param task executable task to run on this region's tick strand
     * @return {@code true} if accepted; {@code false} if rejected due to capacity limit
     */
    public boolean tell(long sourceRegionKey, Runnable task) {
        Objects.requireNonNull(task, "task cannot be null");
        return enqueueInternal(sourceRegionKey, 0L, task);
    }

    /**
     * Submits an asynchronous request-reply task (Actor Ask pattern) with default timeout.
     *
     * @param <T> result type
     * @param sourceRegionKey packed region key of the requesting strand
     * @param taskSupplier supplier computing the result on this region strand
     * @return CompletableFuture resolving to the computed result or failing with timeout
     */
    public <T> CompletableFuture<T> ask(long sourceRegionKey, Supplier<T> taskSupplier) {
        return ask(sourceRegionKey, taskSupplier, DEFAULT_ASK_TIMEOUT_MILLIS);
    }

    /**
     * Submits an asynchronous request-reply task (Actor Ask pattern) with a custom timeout.
     *
     * @param <T> result type
     * @param sourceRegionKey packed region key of the requesting strand
     * @param taskSupplier supplier computing the result on this region strand
     * @param timeoutMillis maximum allowable latency before timing out
     * @return CompletableFuture resolving to the computed result or failing with timeout
     */
    public <T> CompletableFuture<T> ask(long sourceRegionKey, Supplier<T> taskSupplier, long timeoutMillis) {
        Objects.requireNonNull(taskSupplier, "taskSupplier cannot be null");
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("timeoutMillis must be positive: " + timeoutMillis);
        }

        CompletableFuture<T> future = new CompletableFuture<>();
        future.orTimeout(timeoutMillis, TimeUnit.MILLISECONDS);

        CrossRegionAskTask<T> askTask = new CrossRegionAskTask<>(taskSupplier, future);
        boolean enqueued = enqueueInternal(sourceRegionKey, timeoutMillis, askTask);

        if (!enqueued) {
            future.completeExceptionally(
                    new IllegalStateException("Mailbox for region 0x" + Long.toHexString(regionKey) + " is saturated")
            );
        }

        return future;
    }

    private boolean enqueueInternal(long sourceRegionKey, long timeoutMillis, Runnable task) {
        while (true) {
            int current = queueSize.get();
            if (current >= maxQueueCapacity) {
                rejectedCount.incrementAndGet();
                return false;
            }
            if (queueSize.compareAndSet(current, current + 1)) {
                break;
            }
        }

        long requestId = nextRequestId.getAndIncrement();
        CrossRegionEnvelope envelope = new CrossRegionEnvelope(
                requestId,
                sourceRegionKey,
                regionKey,
                System.nanoTime(),
                timeoutMillis,
                task
        );

        queue.offer(envelope);
        return true;
    }

    /**
     * Processes the next pending message in this mailbox.
     * Should only be invoked by the owning region strand thread.
     *
     * @return {@code true} if a message was processed; {@code false} if the mailbox is empty
     */
    public boolean processNext() {
        CrossRegionEnvelope envelope = queue.poll();
        if (envelope == null) {
            return false;
        }

        queueSize.decrementAndGet();
        long now = System.nanoTime();

        if (envelope.isExpired(now)) {
            timeoutCount.incrementAndGet();
            return true;
        }

        try {
            envelope.task().run();
            processedCount.incrementAndGet();
        } catch (Throwable throwable) {
            // Task-level failures are contained to avoid terminating the strand
        }

        return true;
    }

    /**
     * Drains and executes up to {@code maxBatch} pending messages during a tick pass.
     *
     * @param maxBatch maximum number of messages to process
     * @return number of messages handled (including expired ones)
     */
    public int drainBatch(int maxBatch) {
        if (maxBatch <= 0) {
            return 0;
        }

        int count = 0;
        while (count < maxBatch && processNext()) {
            count++;
        }
        return count;
    }

    /**
     * Returns the packed region key associated with this mailbox.
     *
     * @return 64-bit region key
     */
    public long regionKey() {
        return regionKey;
    }

    /**
     * Returns the current number of pending envelopes in the queue.
     *
     * @return pending message count
     */
    public int size() {
        return queueSize.get();
    }

    /**
     * Checks if this mailbox contains no pending messages.
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        return queueSize.get() == 0;
    }

    /**
     * Returns the total count of successfully processed tasks.
     *
     * @return count of processed tasks
     */
    public long processedCount() {
        return processedCount.get();
    }

    /**
     * Returns the total count of messages that timed out before execution.
     *
     * @return count of timed-out messages
     */
    public long timeoutCount() {
        return timeoutCount.get();
    }

    /**
     * Returns the total count of messages rejected due to queue capacity overflow.
     *
     * @return count of rejected messages
     */
    public long rejectedCount() {
        return rejectedCount.get();
    }

    /**
     * Clears all pending envelopes from this mailbox.
     */
    public void clear() {
        CrossRegionEnvelope envelope;
        while ((envelope = queue.poll()) != null) {
            queueSize.decrementAndGet();
        }
    }
}
