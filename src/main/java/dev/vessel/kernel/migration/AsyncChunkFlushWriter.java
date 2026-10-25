package dev.vessel.kernel.migration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Asynchronous disk flush engine persisting immutable chunk snapshots via isolated worker threads.
 * <p>
 * Implements bounded queue backpressure and spatial deduplication throttling to eliminate
 * server lockups, chunk tearing, and memory exhaustion during autosave and world unmount operations.
 * </p>
 * <p>
 * Mitigates: <b>ERR-98</b>, <b>ERR-100</b>, <b>STO-051..100</b>.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class AsyncChunkFlushWriter implements AutoCloseable {

    /** Default worker thread count for background chunk writing. */
    public static final int DEFAULT_WORKER_THREADS = 2;

    /** Default flush sync poll interval in milliseconds. */
    public static final long SYNC_POLL_INTERVAL_MS = 25L;

    private final ChunkStorageSink storageSink;
    private final ChunkFlushQueueThrottler throttler;
    private final BlockingQueue<ImmutableChunkSnapshot> queue;
    private final List<Thread> workerThreads;
    private final AtomicBoolean running;
    private final AtomicLong queuedCounter;
    private final AtomicLong writtenCounter;
    private final AtomicLong deduplicatedCounter;
    private final AtomicLong failureCounter;

    /**
     * Constructs a flush writer with default workers and throttling settings.
     *
     * @param storageSink target storage sink
     */
    public AsyncChunkFlushWriter(ChunkStorageSink storageSink) {
        this(storageSink, DEFAULT_WORKER_THREADS, new ChunkFlushQueueThrottler());
    }

    /**
     * Constructs a flush writer with custom concurrency and throttler.
     *
     * @param storageSink   target storage sink
     * @param workerCount   number of dedicated background I/O threads
     * @param throttler     queue throttler and deduplicator
     */
    public AsyncChunkFlushWriter(ChunkStorageSink storageSink, int workerCount, ChunkFlushQueueThrottler throttler) {
        if (workerCount <= 0) {
            throw new IllegalArgumentException("Worker count must be greater than zero: " + workerCount);
        }
        this.storageSink = Objects.requireNonNull(storageSink, "storageSink cannot be null");
        this.throttler = Objects.requireNonNull(throttler, "throttler cannot be null");
        this.queue = new LinkedBlockingQueue<>(throttler.maxCapacity());
        this.running = new AtomicBoolean(true);
        this.queuedCounter = new AtomicLong(0);
        this.writtenCounter = new AtomicLong(0);
        this.deduplicatedCounter = new AtomicLong(0);
        this.failureCounter = new AtomicLong(0);
        this.workerThreads = new ArrayList<>(workerCount);

        ChunkIoThreadFactory threadFactory = new ChunkIoThreadFactory();
        for (int i = 0; i < workerCount; i++) {
            ChunkFlushWorkerTask task = new ChunkFlushWorkerTask(
                    queue,
                    storageSink,
                    throttler,
                    running,
                    writtenCounter,
                    failureCounter
            );
            Thread worker = threadFactory.newThread(task);
            workerThreads.add(worker);
            worker.start();
        }
    }

    /**
     * Submits an immutable chunk snapshot to the asynchronous flush queue.
     *
     * @param snapshot the chunk snapshot to persist
     * @return true if accepted, false if dropped via throttling
     */
    public boolean submit(ImmutableChunkSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot cannot be null");
        if (!running.get()) {
            throw new AsyncFlushException("Cannot submit snapshot to stopped AsyncChunkFlushWriter");
        }

        boolean superseded = throttler.registerSnapshot(snapshot);
        if (superseded && throttler.shouldThrottle(queue.size())) {
            deduplicatedCounter.incrementAndGet();
        }

        boolean offered = queue.offer(snapshot);
        if (offered) {
            queuedCounter.incrementAndGet();
            return true;
        }

        // Queue is completely full
        deduplicatedCounter.incrementAndGet();
        return false;
    }

    /**
     * Synchronously blocks until all currently pending snapshots have been written to disk.
     *
     * @param timeoutMs maximum wait time in milliseconds
     * @return true if queue drained cleanly within timeout, false otherwise
     */
    public boolean flushAllSync(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!queue.isEmpty() || hasActiveWrites()) {
            if (System.currentTimeMillis() > deadline) {
                return false;
            }
            try {
                Thread.sleep(SYNC_POLL_INTERVAL_MS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }

    private boolean hasActiveWrites() {
        return queuedCounter.get() > (writtenCounter.get() + deduplicatedCounter.get() + failureCounter.get());
    }

    /**
     * Obtains an immutable metrics snapshot of the flush writer.
     *
     * @return current performance metrics
     */
    public ChunkFlushMetrics getMetrics() {
        return new ChunkFlushMetrics(
                queuedCounter.get(),
                writtenCounter.get(),
                deduplicatedCounter.get(),
                failureCounter.get(),
                queue.size()
        );
    }

    @Override
    public void close() {
        if (running.compareAndSet(true, false)) {
            // Drain remaining queue within 5000ms
            flushAllSync(5000L);

            for (Thread worker : workerThreads) {
                worker.interrupt();
            }
        }
    }
}
