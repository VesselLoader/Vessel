package dev.vessel.kernel.migration;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dedicated thread factory producing named background daemon worker threads for chunk I/O operations.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ChunkIoThreadFactory implements ThreadFactory {

    /** Default thread name prefix. */
    public static final String DEFAULT_THREAD_PREFIX = "vessel-chunk-io-";

    private final String prefix;
    private final AtomicInteger threadCounter;

    /**
     * Constructs a thread factory with the default prefix.
     */
    public ChunkIoThreadFactory() {
        this(DEFAULT_THREAD_PREFIX);
    }

    /**
     * Constructs a thread factory with a custom name prefix.
     *
     * @param prefix the thread name prefix
     */
    public ChunkIoThreadFactory(String prefix) {
        this.prefix = Objects.requireNonNull(prefix, "prefix cannot be null");
        this.threadCounter = new AtomicInteger(1);
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable cannot be null");
        Thread thread = new Thread(runnable, prefix + threadCounter.getAndIncrement());
        thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY - 1);
        return thread;
    }
}
