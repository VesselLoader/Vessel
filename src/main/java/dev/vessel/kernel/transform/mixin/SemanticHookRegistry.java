package dev.vessel.kernel.transform.mixin;

import dev.vessel.kernel.ir.VesselSemanticHookRecord;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-speed thread-safe registry caching semantic hooks grouped by target class.
 * Ensures zero-allocation lookups during hot-path classloading bytecode transformation.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class SemanticHookRegistry {

    private final ConcurrentHashMap<String, List<VesselSemanticHookRecord>> hooksByTarget =
            new ConcurrentHashMap<>();

    /**
     * Registers a semantic hook. Automatically orders hooks by descending priority.
     *
     * @param hook the hook record to register
     */
    public void register(VesselSemanticHookRecord hook) {
        Objects.requireNonNull(hook, "hook cannot be null");
        String normalizedTarget = hook.targetOwner().replace('.', '/');

        hooksByTarget.compute(normalizedTarget, (k, existing) -> {
            List<VesselSemanticHookRecord> list = (existing == null)
                    ? new ArrayList<>()
                    : new ArrayList<>(existing);
            list.add(hook);
            list.sort(Comparator.comparingInt(VesselSemanticHookRecord::priority).reversed());
            return Collections.unmodifiableList(list);
        });
    }

    /**
     * Checks if any semantic hooks target the given class name.
     *
     * @param targetClassName class name (internal or binary name)
     * @return true if one or more hooks exist for this class
     */
    public boolean hasHooksFor(String targetClassName) {
        if (targetClassName == null) return false;
        String normalized = targetClassName.replace('.', '/');
        List<VesselSemanticHookRecord> list = hooksByTarget.get(normalized);
        return list != null && !list.isEmpty();
    }

    /**
     * Returns an unmodifiable list of hooks configured for the target class.
     *
     * @param targetClassName class name
     * @return ordered list of hooks, or empty list if none
     */
    public List<VesselSemanticHookRecord> getHooksFor(String targetClassName) {
        if (targetClassName == null) return Collections.emptyList();
        String normalized = targetClassName.replace('.', '/');
        List<VesselSemanticHookRecord> list = hooksByTarget.get(normalized);
        return list != null ? list : Collections.emptyList();
    }

    /**
     * Clears all registered hooks.
     */
    public void clear() {
        hooksByTarget.clear();
    }

    /**
     * Returns the total number of registered semantic hooks across all classes.
     *
     * @return total hook count
     */
    public int getTotalHookCount() {
        int count = 0;
        for (List<VesselSemanticHookRecord> list : hooksByTarget.values()) {
            count += list.size();
        }
        return count;
    }
}
