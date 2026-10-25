package dev.vessel.kernel.optimization.leak;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Memory Leak Fix-inspired lifecycle disposal registry.
 * <p>
 * Modded Minecraft engines suffer from persistent heap leaks caused by static listener lists,
 * cached world references, thread-locals, and lingering actor mailboxes that fail to release
 * when players disconnect or chunks/worlds unload.
 * This registry guarantees deterministic cleanup and unbinding of resources at explicit
 * lifecycle milestones.
 *
 * @version 1.0.1
 */
public final class LifecycleDisposalRegistry {

    private final Map<LifecycleDisposalScope, Map<String, List<AutoCloseable>>> scopedResources;
    private final AtomicLong registeredCount;
    private final AtomicLong disposedCount;

    /**
     * Constructs a new lifecycle disposal registry.
     */
    public LifecycleDisposalRegistry() {
        this.scopedResources = new ConcurrentHashMap<>();
        for (LifecycleDisposalScope scope : LifecycleDisposalScope.values()) {
            this.scopedResources.put(scope, new ConcurrentHashMap<>());
        }
        this.registeredCount = new AtomicLong(0L);
        this.disposedCount = new AtomicLong(0L);
    }

    /**
     * Registers an {@link AutoCloseable} resource to be disposed at a specific lifecycle scope.
     *
     * @param scope    lifecycle scope boundary
     * @param scopeKey unique scoping key (e.g., player UUID, chunk coordinate string, world name)
     * @param resource the resource to close upon disposal
     */
    public void registerCloseable(LifecycleDisposalScope scope, String scopeKey, AutoCloseable resource) {
        Objects.requireNonNull(scope, "scope cannot be null");
        Objects.requireNonNull(scopeKey, "scopeKey cannot be null");
        Objects.requireNonNull(resource, "resource cannot be null");

        Map<String, List<AutoCloseable>> keyMap = scopedResources.get(scope);
        keyMap.compute(scopeKey, (k, list) -> {
            if (list == null) {
                list = new ArrayList<>();
            }
            list.add(resource);
            return list;
        });
        registeredCount.incrementAndGet();
    }

    /**
     * Registers a cleanup {@link Runnable} action to be executed upon scope disposal.
     *
     * @param scope         lifecycle scope boundary
     * @param scopeKey      scoping key
     * @param cleanupAction runnable action to execute
     */
    public void registerAction(LifecycleDisposalScope scope, String scopeKey, Runnable cleanupAction) {
        Objects.requireNonNull(cleanupAction, "cleanupAction cannot be null");
        registerCloseable(scope, scopeKey, cleanupAction::run);
    }

    /**
     * Disposes and unregisters all resources associated with a specific scope and key.
     *
     * @param scope    lifecycle scope
     * @param scopeKey scoping key
     * @return number of resources successfully disposed
     */
    public int dispose(LifecycleDisposalScope scope, String scopeKey) {
        Objects.requireNonNull(scope, "scope cannot be null");
        Objects.requireNonNull(scopeKey, "scopeKey cannot be null");

        Map<String, List<AutoCloseable>> keyMap = scopedResources.get(scope);
        List<AutoCloseable> resources = keyMap.remove(scopeKey);
        if (resources == null || resources.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (AutoCloseable resource : resources) {
            try {
                resource.close();
                count++;
                disposedCount.incrementAndGet();
            } catch (Exception ignored) {
                // Ignore close exceptions to ensure all registered items are processed
            }
        }
        return count;
    }

    /**
     * Disposes and unregisters all resources registered under an entire lifecycle scope.
     *
     * @param scope the lifecycle scope to purge
     * @return total resources disposed
     */
    public int disposeAll(LifecycleDisposalScope scope) {
        Objects.requireNonNull(scope, "scope cannot be null");
        Map<String, List<AutoCloseable>> keyMap = scopedResources.get(scope);
        List<String> keys = new ArrayList<>(keyMap.keySet());
        int total = 0;
        for (String key : keys) {
            total += dispose(scope, key);
        }
        return total;
    }

    /**
     * Disposes all registered resources across all lifecycle scopes.
     *
     * @return total resources disposed
     */
    public int disposeAll() {
        int total = 0;
        for (LifecycleDisposalScope scope : LifecycleDisposalScope.values()) {
            total += disposeAll(scope);
        }
        return total;
    }

    /**
     * Returns total resources ever registered.
     *
     * @return count
     */
    public long getRegisteredCount() {
        return registeredCount.get();
    }

    /**
     * Returns total resources successfully disposed.
     *
     * @return count
     */
    public long getDisposedCount() {
        return disposedCount.get();
    }

    /**
     * Returns the number of currently active registered resources awaiting disposal.
     *
     * @return active count
     */
    public int getActiveCount() {
        int count = 0;
        for (Map<String, List<AutoCloseable>> keyMap : scopedResources.values()) {
            for (List<AutoCloseable> list : keyMap.values()) {
                count += list.size();
            }
        }
        return count;
    }
}
