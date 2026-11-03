package org.bukkit.scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.plugin.Plugin;

/**
 * Runtime implementation of {@code org.bukkit.scheduler.BukkitScheduler} for Bukkit and Spigot
 * plugins running on the VESSEL Sovereign Microkernel.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class BukkitScheduler {

    private final AtomicInteger nextTaskId = new AtomicInteger(1);
    private final List<BukkitTask> activeTasks = new CopyOnWriteArrayList<>();

    /**
     * Constructs a new {@code BukkitScheduler}.
     */
    public BukkitScheduler() {
    }

    /**
     * Schedules a synchronous task to execute on the main server thread.
     *
     * @param plugin owning plugin
     * @param task   task callback
     * @return scheduled {@link BukkitTask} handle
     */
    public BukkitTask runTask(Plugin plugin, Runnable task) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(task, "task must not be null");
        BukkitTask bukkitTask = new BukkitTask(nextTaskId.getAndIncrement(), plugin, true, task);
        activeTasks.add(bukkitTask);
        bukkitTask.executeIfActive();
        return bukkitTask;
    }

    /**
     * Schedules an asynchronous task.
     *
     * @param plugin owning plugin
     * @param task   task callback
     * @return scheduled {@link BukkitTask} handle
     */
    public BukkitTask runTaskAsynchronously(Plugin plugin, Runnable task) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(task, "task must not be null");
        BukkitTask bukkitTask = new BukkitTask(nextTaskId.getAndIncrement(), plugin, false, task);
        activeTasks.add(bukkitTask);
        bukkitTask.executeIfActive();
        return bukkitTask;
    }

    /**
     * Schedules a synchronous task to execute after the specified tick delay.
     *
     * @param plugin     owning plugin
     * @param task       task callback
     * @param delayTicks delay in server ticks before execution
     * @return scheduled {@link BukkitTask} handle
     */
    public BukkitTask runTaskLater(Plugin plugin, Runnable task, long delayTicks) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(task, "task must not be null");
        BukkitTask bukkitTask = new BukkitTask(nextTaskId.getAndIncrement(), plugin, true, task);
        activeTasks.add(bukkitTask);
        if (delayTicks <= 0L) {
            bukkitTask.executeIfActive();
        }
        return bukkitTask;
    }

    /**
     * Schedules a repeating synchronous timer task.
     *
     * @param plugin      owning plugin
     * @param task        task callback
     * @param delayTicks  initial delay in server ticks
     * @param periodTicks repeating period in server ticks
     * @return scheduled {@link BukkitTask} handle
     */
    public BukkitTask runTaskTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(task, "task must not be null");
        BukkitTask bukkitTask = new BukkitTask(nextTaskId.getAndIncrement(), plugin, true, task);
        activeTasks.add(bukkitTask);
        bukkitTask.executeIfActive();
        return bukkitTask;
    }

    /**
     * Cancels a scheduled task by its numeric task ID.
     *
     * @param taskId numeric task identifier
     */
    public void cancelTask(int taskId) {
        for (BukkitTask task : activeTasks) {
            if (task.getTaskId() == taskId) {
                task.cancel();
            }
        }
    }

    /**
     * Cancels all tasks owned by the specified plugin.
     *
     * @param plugin target plugin
     */
    public void cancelTasks(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        for (BukkitTask task : activeTasks) {
            if (plugin.equals(task.getOwner())) {
                task.cancel();
            }
        }
    }

    /**
     * Returns an unmodifiable snapshot of all non-cancelled tasks registered with this scheduler.
     *
     * @return list of pending tasks
     */
    public List<BukkitTask> getPendingTasks() {
        List<BukkitTask> pending = new ArrayList<>();
        for (BukkitTask task : activeTasks) {
            if (!task.isCancelled()) {
                pending.add(task);
            }
        }
        return Collections.unmodifiableList(pending);
    }

    /**
     * Executes all non-cancelled tasks currently in {@code activeTasks}.
     *
     * @return number of tasks executed
     */
    public int drainPendingTasks() {
        int executed = 0;
        for (BukkitTask task : activeTasks) {
            if (!task.isCancelled()) {
                task.executeIfActive();
                executed++;
            }
        }
        return executed;
    }
}
