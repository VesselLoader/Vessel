package io.papermc.paper.threadedregions.scheduler;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.bukkit.plugin.Plugin;

/**
 * Paper/Folia off-tick asynchronous scheduler ({@code io.papermc.paper.threadedregions.scheduler.AsyncScheduler})
 * on the VESSEL Sovereign Microkernel.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class AsyncScheduler {

    private final AtomicInteger executedAsyncTaskCount = new AtomicInteger();
    private final List<ScheduledTask> asyncTasks = new CopyOnWriteArrayList<>();

    /**
     * Constructs a new {@code AsyncScheduler}.
     */
    public AsyncScheduler() {
    }

    /**
     * Immediately schedules and executes an asynchronous task.
     *
     * @param plugin owning plugin
     * @param task   consumer receiving the {@link ScheduledTask} handle
     * @return scheduled task handle
     */
    public ScheduledTask runNow(Plugin plugin, Consumer<ScheduledTask> task) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(task, "task must not be null");
        ScheduledTask st = new ScheduledTask(plugin, false);
        asyncTasks.add(st);
        executedAsyncTaskCount.incrementAndGet();
        task.accept(st);
        return st;
    }

    /**
     * Cancels all asynchronous tasks owned by the specified plugin.
     *
     * @param plugin target plugin
     */
    public void cancelTasks(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        for (ScheduledTask st : asyncTasks) {
            if (plugin.equals(st.getOwningPlugin())) {
                st.cancel();
            }
        }
    }

    /**
     * Returns the total number of asynchronous tasks executed.
     *
     * @return executed async task count
     */
    public int getExecutedAsyncTaskCount() {
        return executedAsyncTaskCount.get();
    }
}
