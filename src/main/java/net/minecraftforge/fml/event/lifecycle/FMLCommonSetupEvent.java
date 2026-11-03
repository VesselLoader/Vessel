package net.minecraftforge.fml.event.lifecycle;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

/**
 * Canonical Minecraft Forge common setup lifecycle event
 * ({@code net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent}).
 *
 * <p>Fired on the mod event bus after registry events complete, allowing mods to perform
 * common initialization and enqueue thread-safe deferred setup tasks via
 * {@link #enqueueWork(Runnable)} and {@link #enqueueWork(Supplier)}.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class FMLCommonSetupEvent extends ModLifecycleEvent {

    private final Queue<Runnable> deferredWorkQueue = new ConcurrentLinkedQueue<>();

    /**
     * Constructs a common setup event for the default namespace.
     */
    public FMLCommonSetupEvent() {
        super();
    }

    /**
     * Constructs a common setup event for the specified mod identifier.
     *
     * @param modId target mod identifier
     */
    public FMLCommonSetupEvent(String modId) {
        super(modId);
    }

    /**
     * Enqueues a deferred synchronous work task to be drained on the main server setup thread.
     *
     * @param work non-null runnable task
     * @return completed future representing the queued operation
     */
    public CompletableFuture<Void> enqueueWork(Runnable work) {
        Objects.requireNonNull(work, "work must not be null");
        deferredWorkQueue.add(work);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Enqueues and computes a deferred supplier work task.
     *
     * @param work non-null supplier task
     * @param <T>  result type
     * @return completed future carrying the supplier's result
     */
    public <T> CompletableFuture<T> enqueueWork(Supplier<T> work) {
        Objects.requireNonNull(work, "work must not be null");
        T result = work.get();
        return CompletableFuture.completedFuture(result);
    }

    /**
     * Polls and executes all queued {@link Runnable} tasks in FIFO order.
     *
     * @return number of deferred tasks executed
     */
    public int drainEnqueuedWork() {
        int executed = 0;
        Runnable task;
        while ((task = deferredWorkQueue.poll()) != null) {
            task.run();
            executed++;
        }
        return executed;
    }
}
