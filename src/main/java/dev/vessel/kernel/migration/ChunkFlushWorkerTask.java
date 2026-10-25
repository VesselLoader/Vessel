package dev.vessel.kernel.migration;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Worker execution task continually consuming chunk snapshots from an asynchronous queue
 * and persisting them to the underlying storage sink.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ChunkFlushWorkerTask implements Runnable {

    /** Maximum poll timeout in milliseconds. */
    public static final long POLL_TIMEOUT_MS = 100L;

    private final BlockingQueue<ImmutableChunkSnapshot> queue;
    private final ChunkStorageSink storageSink;
    private final ChunkFlushQueueThrottler throttler;
    private final AtomicBoolean running;
    private final AtomicLong writtenCounter;
    private final AtomicLong failureCounter;

    /**
     * Constructs the worker task with its associated synchronization handles.
     *
     * @param queue          the inbound snapshot queue
     * @param storageSink    the storage sink target
     * @param throttler      the queue throttler
     * @param running        lifecycle running flag
     * @param writtenCounter metric counter for written chunks
     * @param failureCounter metric counter for failed chunk writes
     */
    public ChunkFlushWorkerTask(
            BlockingQueue<ImmutableChunkSnapshot> queue,
            ChunkStorageSink storageSink,
            ChunkFlushQueueThrottler throttler,
            AtomicBoolean running,
            AtomicLong writtenCounter,
            AtomicLong failureCounter
    ) {
        this.queue = Objects.requireNonNull(queue, "queue cannot be null");
        this.storageSink = Objects.requireNonNull(storageSink, "storageSink cannot be null");
        this.throttler = Objects.requireNonNull(throttler, "throttler cannot be null");
        this.running = Objects.requireNonNull(running, "running cannot be null");
        this.writtenCounter = Objects.requireNonNull(writtenCounter, "writtenCounter cannot be null");
        this.failureCounter = Objects.requireNonNull(failureCounter, "failureCounter cannot be null");
    }

    @Override
    public void run() {
        while (running.get() || !queue.isEmpty()) {
            try {
                ImmutableChunkSnapshot snapshot = queue.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (snapshot != null) {
                    processSnapshot(snapshot);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void processSnapshot(ImmutableChunkSnapshot snapshot) {
        try {
            storageSink.writeChunk(snapshot);
            writtenCounter.incrementAndGet();
        } catch (Throwable ex) {
            failureCounter.incrementAndGet();
        } finally {
            throttler.deregisterSnapshot(snapshot);
        }
    }
}
