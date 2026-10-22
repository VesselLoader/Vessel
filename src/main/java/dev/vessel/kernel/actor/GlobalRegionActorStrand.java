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
 * Pinned execution strand representing the Global Region in the VESSEL microkernel.
 * <p>
 * Emulates the canonical single-threaded Minecraft server tick loop for legacy Bukkit, Spigot,
 * and Paper plugins that have not declared Folia multi-threading support.
 * </p>
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-4)
 */
public class GlobalRegionActorStrand implements AutoCloseable {

    private final ActorMailboxQueue<Runnable> mailbox = new ActorMailboxQueue<>();
    private final ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private volatile Thread executionThread;

    public GlobalRegionActorStrand() {
        ThreadFactory factory = r -> {
            Thread thread = new Thread(r, "vessel-global-region-strand");
            thread.setDaemon(true);
            this.executionThread = thread;
            return thread;
        };
        this.executor = Executors.newSingleThreadExecutor(factory);
    }

    /**
     * Posts a fire-and-forget task to be executed on the global strand.
     *
     * @param task the runnable task
     */
    public void post(Runnable task) {
        Objects.requireNonNull(task, "task cannot be null");
        if (!running.get()) {
            throw new IllegalStateException("GlobalRegionActorStrand is stopped");
        }
        mailbox.send(task);
        executor.execute(() -> {
            mailbox.poll().ifPresent(Runnable::run);
        });
    }

    /**
     * Submits a request-reply task to be executed on the global strand.
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
     * Checks if the currently executing thread is the global region execution thread.
     *
     * @return true if called from within the global strand
     */
    public boolean isCurrentStrand() {
        return Thread.currentThread() == executionThread;
    }

    /**
     * Initiates orderly shutdown of the global strand.
     */
    public void shutdown() {
        if (running.compareAndSet(true, false)) {
            executor.shutdown();
        }
    }

    /**
     * Returns whether the global strand has been shut down.
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
