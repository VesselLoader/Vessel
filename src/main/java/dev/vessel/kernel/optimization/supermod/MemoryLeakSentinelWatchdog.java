package dev.vessel.kernel.optimization.supermod;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tracks unloaded worlds, dimensions, and plugin/mod classloaders via {@link WeakReference}
 * sentinels to detect retained dead heap references after dimension or plugin unload.
 *
 * @version 1.0.1
 */
public final class MemoryLeakSentinelWatchdog {

    private final Map<String, List<WeakReference<Object>>> trackedResources =
            new ConcurrentHashMap<>();

    /**
     * Registers an unloaded resource instance under its owning module ID for post-unload retention
     * tracking.
     *
     * @param ownerModuleId    the mod or plugin identifier
     * @param resourceInstance the unloaded object instance to monitor via weak reference
     */
    public void trackUnloadedResource(String ownerModuleId, Object resourceInstance) {
        Objects.requireNonNull(ownerModuleId, "ownerModuleId must not be null");
        Objects.requireNonNull(resourceInstance, "resourceInstance must not be null");
        trackedResources
                .computeIfAbsent(ownerModuleId, ignored -> new CopyOnWriteArrayList<>())
                .add(new WeakReference<>(resourceInstance));
    }

    /**
     * Scans all tracked unloaded resources and returns the module IDs whose unloaded objects are
     * still strongly reachable on the heap.
     *
     * @return the list of owner module IDs with retained references
     */
    public List<String> scanForRetainedDeadReferences() {
        List<String> retainedModules = new ArrayList<>();
        Iterator<Map.Entry<String, List<WeakReference<Object>>>> it = trackedResources.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, List<WeakReference<Object>>> entry = it.next();
            List<WeakReference<Object>> refs = entry.getValue();
            refs.removeIf(ref -> ref.get() == null);
            if (!refs.isEmpty()) {
                retainedModules.add(entry.getKey());
            } else {
                it.remove();
            }
        }
        return List.copyOf(retainedModules);
    }

    /**
     * Clears tracking for a specific module.
     *
     * @param ownerModuleId the mod or plugin identifier
     */
    public void clearModule(String ownerModuleId) {
        if (ownerModuleId != null) {
            trackedResources.remove(ownerModuleId);
        }
    }

    /**
     * Clears all tracked sentinels.
     */
    public void clearAll() {
        trackedResources.clear();
    }
}
