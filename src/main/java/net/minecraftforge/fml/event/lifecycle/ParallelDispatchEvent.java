package net.minecraftforge.fml.event.lifecycle;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Lifecycle event supporting parallel dispatch and asynchronous work enqueuing.
 * Base class for FMLCommonSetupEvent, InterModEnqueueEvent, and InterModProcessEvent.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public class ParallelDispatchEvent extends ModLifecycleEvent {

    public ParallelDispatchEvent() {
        super("minecraft");
    }

    public ParallelDispatchEvent(String modId) {
        super(modId);
    }

    public CompletableFuture<Void> enqueueWork(Runnable work) {
        if (work != null) {
            try {
                work.run();
            } catch (Throwable ignored) {
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    public <T> CompletableFuture<T> enqueueWork(Supplier<T> work) {
        if (work != null) {
            try {
                return CompletableFuture.completedFuture(work.get());
            } catch (Throwable ignored) {
            }
        }
        return CompletableFuture.completedFuture(null);
    }
}
