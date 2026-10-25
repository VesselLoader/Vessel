package dev.vessel.kernel.tick;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Executable task wrapper that resolves an asynchronous Ask-pattern CompletableFuture
 * upon execution on the destination Folia region strand.
 * <p>
 * Ensures zero nested/anonymous class allocations and strict Single Responsibility Principle.
 * </p>
 * <p>
 * Directly resolves ERR-31, ERR-32, and FOL-041..060.
 * </p>
 *
 * @param <T> result payload type
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class CrossRegionAskTask<T> implements Runnable {

    private final Supplier<T> taskSupplier;
    private final CompletableFuture<T> future;

    /**
     * Constructs a new CrossRegionAskTask.
     *
     * @param taskSupplier supplier producing the computed result
     * @param future future to complete when execution finishes
     */
    public CrossRegionAskTask(Supplier<T> taskSupplier, CompletableFuture<T> future) {
        this.taskSupplier = Objects.requireNonNull(taskSupplier, "taskSupplier cannot be null");
        this.future = Objects.requireNonNull(future, "future cannot be null");
    }

    @Override
    public void run() {
        if (future.isDone()) {
            return;
        }
        try {
            T result = taskSupplier.get();
            future.complete(result);
        } catch (Throwable throwable) {
            future.completeExceptionally(throwable);
        }
    }
}
