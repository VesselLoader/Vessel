package dev.vessel.kernel.runtime;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Thread-safe registry for cross-loader synthetic interface injections.
 * Allows mods/plugins from Fabric, Quilt, Forge, or NeoForge to inject
 * custom duck-typing interfaces into target game or third-party classes.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class InterfaceInjectionRegistry {

    private final ConcurrentHashMap<String, CopyOnWriteArraySet<String>> injectionTable =
            new ConcurrentHashMap<>();

    /**
     * Registers a synthetic interface to be injected into a target class.
     *
     * @param targetClassName the fully-qualified class name of the target (e.g. "net.minecraft.world.entity.Entity")
     * @param interfaceName the fully-qualified interface name to inject (e.g. "com.example.mod.DuckEntity")
     */
    public void register(String targetClassName, String interfaceName) {
        Objects.requireNonNull(targetClassName, "targetClassName cannot be null");
        Objects.requireNonNull(interfaceName, "interfaceName cannot be null");

        String normalizedTarget = normalizeInternalName(targetClassName);
        String normalizedInterface = normalizeInternalName(interfaceName);

        injectionTable.computeIfAbsent(normalizedTarget, k -> new CopyOnWriteArraySet<>())
                .add(normalizedInterface);
    }

    /**
     * Checks if the target class has any registered synthetic interface injections.
     *
     * @param targetClassName the class name to inspect
     * @return true if one or more interfaces are scheduled for injection
     */
    public boolean hasInjections(String targetClassName) {
        if (targetClassName == null) {
            return false;
        }
        Set<String> set = injectionTable.get(normalizeInternalName(targetClassName));
        return set != null && !set.isEmpty();
    }

    /**
     * Returns an unmodifiable set of all interface internal names registered for the target class.
     *
     * @param targetClassName the class name to inspect
     * @return set of interface internal names, or empty set if none registered
     */
    public Set<String> getInjectedInterfaces(String targetClassName) {
        if (targetClassName == null) {
            return Collections.emptySet();
        }
        Set<String> set = injectionTable.get(normalizeInternalName(targetClassName));
        if (set == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(set);
    }

    /**
     * Clears all registered interface injections.
     */
    public void clear() {
        injectionTable.clear();
    }

    /**
     * Returns the total number of distinct target classes currently configured for injection.
     *
     * @return count of configured target classes
     */
    public int getTargetClassCount() {
        return injectionTable.size();
    }

    private static String normalizeInternalName(String className) {
        return className.replace('.', '/');
    }
}
