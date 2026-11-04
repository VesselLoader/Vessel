package dev.vessel.kernel.optimization.supermod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Debounces rapid Applied Energistics 2 and Refined Storage grid topology rebuild requests caused
 * by cable placement or chunk boundary loading into a single deferred rebuild per quiet window.
 *
 * <p>Optimized with early-exit zero-allocation fast-paths on idle ticks.</p>
 *
 * @version 1.0.2
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
        if (lastScheduledTimestampMs.isEmpty()) {
            return 0;
        }

        int executed = 0;
        List<Long> expiredGridIds = null;
        for (Long gridId : lastScheduledTimestampMs.keySet()) {
            Long scheduledAt = lastScheduledTimestampMs.get(gridId);
            if (scheduledAt != null && currentTimestampMs - scheduledAt.longValue() >= debounceWindowMs) {
                if (expiredGridIds == null) {
                    expiredGridIds = new ArrayList<>();
                }
                expiredGridIds.add(gridId);
            }
        }

        if (expiredGridIds != null) {
            int count = expiredGridIds.size();
            for (int i = 0; i < count; i++) {
                Long gridId = expiredGridIds.get(i);
                lastScheduledTimestampMs.remove(gridId);
                Runnable task = pendingRebuildTasks.remove(gridId);
                if (task != null) {
                    try {
                        task.run();
                    } catch (Throwable ignored) {
                    }
                    executed++;
                }
            }
        }
        return executed;
    }

    /**
     * Cancels a pending debounced rebuild task for a storage grid.
     *
     * @param gridId the storage network grid identifier
     */
    public void cancelScheduledRebuild(long gridId) {
        lastScheduledTimestampMs.remove(gridId);
        pendingRebuildTasks.remove(gridId);
    }

    /**
     * Clears all pending debounced rebuild tasks.
     */
    public void clear() {
        lastScheduledTimestampMs.clear();
        pendingRebuildTasks.clear();
    }
}
