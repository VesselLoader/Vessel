package dev.vessel.kernel.actor;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Dedicated execution strand governing a 16x16 chunk world region in the VESSEL microkernel.
 * <p>
 * Ensures that all block modifications, entity ticks, and tile entity states within this
 * coordinate region are strictly serialized and executed without race conditions.
 * </p>
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-4)
 */
public class RegionalActorStrand implements AutoCloseable {

    private final RegionCoordinateHash regionHash;
    private final ActorMailboxQueue<Runnable> mailbox = new ActorMailboxQueue<>();
    private final ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private volatile Thread executionThread;

    public RegionalActorStrand(RegionCoordinateHash regionHash) {
        this.regionHash = Objects.requireNonNull(regionHash, "regionHash cannot be null");
        ThreadFactory factory = r -> {
            Thread thread = new Thread(r, "vessel-region-" + regionHash.regionX() + "-" + regionHash.regionZ());
            thread.setDaemon(true);
            this.executionThread = thread;
            return thread;
        };
        this.executor = Executors.newSingleThreadExecutor(factory);
    }

    /**
     * Returns the 16x16 chunk region coordinate hash assigned to this strand.
     *
     * @return the RegionCoordinateHash
     */
    public RegionCoordinateHash getRegionHash() {
        return regionHash;
    }

    /**
     * Posts a fire-and-forget task to be executed on this region's strand.
     *
     * @param task the runnable task
     */
    public void post(Runnable task) {
        Objects.requireNonNull(task, "task cannot be null");
        if (!running.get()) {
            throw new IllegalStateException("RegionalActorStrand is stopped for region: " + regionHash);
        }
        mailbox.send(task);
        executor.execute(() -> {
            mailbox.poll().ifPresent(Runnable::run);
        });
    }

    /**
     * Submits a request-reply task to be executed on this region's strand, returning a CompletableFuture.
     *
     * @param <T> result type
     * @param task supplier generating the result
     * @return CompletableFuture holding the result
     */
    public <T> CompletableFuture<T> ask(Supplier<T> task) {
        Objects.requireNonNull(task, "task cannot be null");
        CompletableFuture<T> future = new CompletableFuture<>();
        post(() -> {
            try {
                future.complete(task.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    /**
     * Checks if the currently executing thread is this region's assigned execution thread.
     *
     * @return true if called from within this strand
     */
    public boolean isCurrentStrand() {
        return Thread.currentThread() == executionThread;
    }

    /**
     * Initiates orderly shutdown of the strand executor.
     */
    public void shutdown() {
        if (running.compareAndSet(true, false)) {
            executor.shutdown();
        }
    }

    /**
     * Returns whether the strand executor has been shut down.
     *
     * @return true if shut down
     */
    public boolean isShutdown() {
        return !running.get() || executor.isShutdown();
    }

    @Override
    public void close() {
        shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
