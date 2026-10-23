package dev.vessel.kernel.optimization.supermod;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Debounces rapid Applied Energistics 2 and Refined Storage grid topology rebuild requests caused
 * by cable placement or chunk boundary loading into a single deferred rebuild per quiet window.
 *
 * @version 1.0.0
 */
public final class StorageNetworkDebounceCoordinator {

    private final Map<Long, Long> lastScheduledTimestampMs = new ConcurrentHashMap<>();
    private final Map<Long, Runnable> pendingRebuildTasks = new ConcurrentHashMap<>();

    /**
     * Schedules or updates a debounced storage grid topology rebuild task.
     *
     * @param gridId             the storage network grid identifier
     * @param currentTimestampMs the current monotonic timestamp in milliseconds
     * @param rebuildTask        the topology rebuild action
     */
    public void scheduleGridTopologyRebuild(
            long gridId,
            long currentTimestampMs,
            Runnable rebuildTask) {
        Objects.requireNonNull(rebuildTask, "rebuildTask must not be null");
        lastScheduledTimestampMs.put(gridId, currentTimestampMs);
        pendingRebuildTasks.put(gridId, rebuildTask);
    }

    /**
     * Flushes and executes all pending grid rebuilds whose quiet window has elapsed.
     *
     * @param currentTimestampMs the current monotonic timestamp in milliseconds
     * @param debounceWindowMs   the minimum quiet duration in milliseconds before executing
     * @return the number of storage grid rebuilds executed
     */
    public int flushDebouncedGridRebuilds(long currentTimestampMs, long debounceWindowMs) {
        int executed = 0;
        Iterator<Map.Entry<Long, Long>> iterator = lastScheduledTimestampMs.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, Long> entry = iterator.next();
            long gridId = entry.getKey();
            long scheduledAt = entry.getValue();
            if (currentTimestampMs - scheduledAt >= debounceWindowMs) {
                iterator.remove();
                Runnable task = pendingRebuildTasks.remove(gridId);
                if (task != null) {
                    task.run();
                    executed++;
                }
            }
        }
        return executed;
    }
}
