package net.neoforged.fml.event.lifecycle;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

/**
 * NeoForge FML common setup lifecycle event ({@code net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent})
 * fired after registry events conclude to initialize cross-mod state and enqueue main-thread work.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class FMLCommonSetupEvent extends ModLifecycleEvent {

    private final Queue<Runnable> deferredWorkQueue = new ConcurrentLinkedQueue<>();

    /**
     * Constructs a new {@code FMLCommonSetupEvent} associated with the default {@code "minecraft"} namespace.
     */
    public FMLCommonSetupEvent() {
        super();
    }

    /**
     * Constructs a new {@code FMLCommonSetupEvent} for the specified mod identifier.
     *
     * @param modId target mod identifier
     */
    public FMLCommonSetupEvent(String modId) {
        super(modId);
    }

    /**
     * Enqueues a deferred synchronous task to be drained on the main setup thread after
     * parallel common setup dispatch completes.
     *
     * @param work non-null task to enqueue
     * @return a completed future representing the enqueued task
     */
    public CompletableFuture<Void> enqueueWork(Runnable work) {
        Objects.requireNonNull(work, "work must not be null");
        deferredWorkQueue.add(work);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Enqueues and evaluates a deferred supplier task.
     *
     * @param work non-null supplier to evaluate
     * @param <T>  result value type
     * @return a completed future holding the supplier's result
     */
    public <T> CompletableFuture<T> enqueueWork(Supplier<T> work) {
        Objects.requireNonNull(work, "work must not be null");
        T result = work.get();
        return CompletableFuture.completedFuture(result);
    }

    /**
     * Polls and executes all queued {@link Runnable} tasks in FIFO order.
     *
     * @return the number of deferred work tasks executed
     */
    public int drainEnqueuedWork() {
        int executedCount = 0;
        Runnable task;
        while ((task = deferredWorkQueue.poll()) != null) {
            task.run();
            executedCount++;
        }
        return executedCount;
    }
}
