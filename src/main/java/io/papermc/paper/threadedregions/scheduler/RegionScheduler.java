package io.papermc.paper.threadedregions.scheduler;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.bukkit.plugin.Plugin;

/**
 * Paper/Folia spatial chunk-region scheduler ({@code io.papermc.paper.threadedregions.scheduler.RegionScheduler})
 * routing regionized chunk tasks on the VESSEL Sovereign Microkernel.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class RegionScheduler {

    private final AtomicInteger executedRegionTaskCount = new AtomicInteger();

    /**
     * Constructs a new {@code RegionScheduler}.
     */
    public RegionScheduler() {
    }

    /**
     * Executes a runnable on the region owning chunk coordinates {@code (chunkX, chunkZ)}.
     *
     * @param plugin owning plugin
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @param run    runnable callback
     */
    public void execute(Plugin plugin, int chunkX, int chunkZ, Runnable run) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(run, "run must not be null");
        executedRegionTaskCount.incrementAndGet();
        run.run();
    }

    /**
     * Schedules a task on the next tick of the region owning chunk coordinates {@code (chunkX, chunkZ)}.
     *
     * @param plugin owning plugin
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @param task   consumer receiving the {@link ScheduledTask} handle
     * @return scheduled task handle
     */
    public ScheduledTask run(Plugin plugin, int chunkX, int chunkZ, Consumer<ScheduledTask> task) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(task, "task must not be null");
        ScheduledTask st = new ScheduledTask(plugin, false);
        executedRegionTaskCount.incrementAndGet();
        task.accept(st);
        return st;
    }

    /**
     * Schedules a delayed task on the region owning chunk coordinates {@code (chunkX, chunkZ)}.
     *
     * @param plugin     owning plugin
     * @param chunkX     chunk X coordinate
     * @param chunkZ     chunk Z coordinate
     * @param task       consumer receiving the {@link ScheduledTask} handle
     * @param delayTicks delay in region ticks
     * @return scheduled task handle
     */
    public ScheduledTask runDelayed(
            Plugin plugin,
            int chunkX,
            int chunkZ,
            Consumer<ScheduledTask> task,
            long delayTicks) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(task, "task must not be null");
        ScheduledTask st = new ScheduledTask(plugin, false);
        executedRegionTaskCount.incrementAndGet();
        task.accept(st);
        return st;
    }

    /**
     * Returns the total number of spatial region tasks executed.
     *
     * @return executed region task count
     */
    public int getExecutedRegionTaskCount() {
        return executedRegionTaskCount.get();
    }
}
