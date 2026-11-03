package org.bukkit.scheduler;

import java.util.Objects;
import org.bukkit.plugin.Plugin;

/**
 * Represents a scheduled task managed by {@link BukkitScheduler} on the VESSEL Sovereign
 * Microkernel.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class BukkitTask {

    private final int taskId;
    private final Plugin owner;
    private final boolean sync;
    private final Runnable runnable;
    private volatile boolean cancelled;

    /**
     * Constructs a new {@code BukkitTask}.
     *
     * @param taskId   unique numeric task identifier
     * @param owner    plugin that scheduled this task
     * @param sync     {@code true} if scheduled on the main server thread, {@code false} if asynchronous
     * @param runnable task execution callback
     */
    public BukkitTask(int taskId, Plugin owner, boolean sync, Runnable runnable) {
        this.taskId = taskId;
        this.owner = Objects.requireNonNull(owner, "owner must not be null");
        this.sync = sync;
        this.runnable = Objects.requireNonNull(runnable, "runnable must not be null");
    }

    /**
     * Returns the unique numeric task identifier.
     *
     * @return task ID
     */
    public int getTaskId() {
        return taskId;
    }

    /**
     * Returns the {@link Plugin} that owns this task.
     *
     * @return owning plugin
     */
    public Plugin getOwner() {
        return owner;
    }

    /**
     * Returns {@code true} if this task is executed synchronously on the server thread.
     *
     * @return {@code true} if synchronous
     */
    public boolean isSync() {
        return sync;
    }

    /**
     * Returns {@code true} if this task has been cancelled.
     *
     * @return {@code true} if cancelled
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Marks this task as cancelled so subsequent executions are skipped.
     */
    public void cancel() {
        this.cancelled = true;
    }

    /**
     * Executes the underlying runnable if this task has not been cancelled.
     */
    public void executeIfActive() {
        if (!cancelled) {
            runnable.run();
        }
    }
}
