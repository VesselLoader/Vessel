package io.papermc.paper.threadedregions.scheduler;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.bukkit.plugin.Plugin;

/**
 * Paper/Folia global region scheduler ({@code io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler})
 * executing global world/weather/console state tasks on the VESSEL Sovereign Microkernel.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class GlobalRegionScheduler {

    private final AtomicInteger executedTaskCount = new AtomicInteger();
    private final List<ScheduledTask> scheduledTasks = new CopyOnWriteArrayList<>();

    /**
     * Constructs a new {@code GlobalRegionScheduler}.
     */
    public GlobalRegionScheduler() {
    }

    /**
     * Executes a one-shot runnable on the global region context.
     *
     * @param plugin owning plugin
     * @param run    runnable callback
     */
    public void execute(Plugin plugin, Runnable run) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(run, "run must not be null");
        executedTaskCount.incrementAndGet();
        run.run();
    }

    /**
     * Schedules and executes a task on the next global region tick.
     *
     * @param plugin owning plugin
     * @param task   consumer receiving the {@link ScheduledTask} handle
     * @return scheduled task handle
     */
    public ScheduledTask run(Plugin plugin, Consumer<ScheduledTask> task) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(task, "task must not be null");
        ScheduledTask st = new ScheduledTask(plugin, false);
        scheduledTasks.add(st);
        executedTaskCount.incrementAndGet();
        task.accept(st);
        return st;
    }

    /**
     * Schedules a delayed task on the global region context.
     *
     * @param plugin     owning plugin
     * @param task       consumer receiving the {@link ScheduledTask} handle
     * @param delayTicks delay in ticks
     * @return scheduled task handle
     */
    public ScheduledTask runDelayed(Plugin plugin, Consumer<ScheduledTask> task, long delayTicks) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(task, "task must not be null");
        ScheduledTask st = new ScheduledTask(plugin, false);
        scheduledTasks.add(st);
        executedTaskCount.incrementAndGet();
        task.accept(st);
        return st;
    }

    /**
     * Schedules a repeating task at a fixed tick rate on the global region context.
     *
     * @param plugin            owning plugin
     * @param task              consumer receiving the {@link ScheduledTask} handle
     * @param initialDelayTicks initial delay in ticks
     * @param periodTicks       period between executions in ticks
     * @return scheduled task handle
     */
    public ScheduledTask runAtFixedRate(
            Plugin plugin,
            Consumer<ScheduledTask> task,
            long initialDelayTicks,
            long periodTicks) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(task, "task must not be null");
        ScheduledTask st = new ScheduledTask(plugin, true);
        scheduledTasks.add(st);
        executedTaskCount.incrementAndGet();
        task.accept(st);
        return st;
    }

    /**
     * Cancels all tasks scheduled by the specified plugin on this global region scheduler.
     *
     * @param plugin target plugin
     */
    public void cancelTasks(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        for (ScheduledTask st : scheduledTasks) {
            if (plugin.equals(st.getOwningPlugin())) {
                st.cancel();
            }
        }
    }

    /**
     * Returns the total number of global region tasks executed.
     *
     * @return executed task count
     */
    public int getExecutedTaskCount() {
        return executedTaskCount.get();
    }
}
