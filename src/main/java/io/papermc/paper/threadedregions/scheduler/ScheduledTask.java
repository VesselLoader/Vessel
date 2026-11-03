package io.papermc.paper.threadedregions.scheduler;

import java.util.Objects;
import org.bukkit.plugin.Plugin;

/**
 * Represents a task scheduled on one of Paper/Folia's threaded region or asynchronous schedulers.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ScheduledTask {

    private final Plugin owningPlugin;
    private final boolean repeatingTask;
    private volatile boolean cancelled;

    /**
     * Constructs a new Folia/Paper {@code ScheduledTask}.
     *
     * @param owningPlugin  plugin that scheduled this task
     * @param repeatingTask {@code true} if this task repeats at a fixed rate
     */
    public ScheduledTask(Plugin owningPlugin, boolean repeatingTask) {
        this.owningPlugin = Objects.requireNonNull(owningPlugin, "owningPlugin must not be null");
        this.repeatingTask = repeatingTask;
    }

    /**
     * Returns the {@link Plugin} that scheduled this task.
     *
     * @return owning plugin
     */
    public Plugin getOwningPlugin() {
        return owningPlugin;
    }

    /**
     * Returns {@code true} if this task is a fixed-rate repeating task.
     *
     * @return {@code true} if repeating
     */
    public boolean isRepeatingTask() {
        return repeatingTask;
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
     * Marks this scheduled task as cancelled.
     */
    public void cancel() {
        this.cancelled = true;
    }
}
