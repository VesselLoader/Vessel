package dev.vessel.kernel.adapter.bukkit;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Isolates and bridges ClassLoaders for Bukkit, Spigot, and Paper plugins with the VESSEL
 * microkernel module classloader hierarchy.
 *
 * <p>Enforces strict module security boundaries (preventing illegal reflection into kernel internals)
 * while providing cross-plugin dependency class resolution with cycle recursion guards.</p>
 *
 * <p>Quells: ERR-62, ECO-081..090.</p>
 * <p>Strictly adheres to Rule 17 (zero inner, nested, or anonymous classes).</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class BukkitPluginClassLoaderBridge {

    /**
     * Default protected package prefixes forbidden from direct Bukkit plugin resolution.
     */
    public static final String[] FORBIDDEN_KERNEL_PREFIXES = {
            "dev.vessel.kernel.vfs.",
            "dev.vessel.kernel.ir.",
            "dev.vessel.kernel.runtime.Synthetic",
            "dev.vessel.bootstrap.mojang.Live"
    };

    private final Map<String, ClassLoader> pluginLoaders = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> pluginDependencies = new ConcurrentHashMap<>();
    private final ThreadLocal<Set<String>> lookupRecursionGuard = new ThreadLocal<>();
    private volatile boolean globalFallbackEnabled = false;

    /**
     * Constructs a new BukkitPluginClassLoaderBridge.
     */
    public BukkitPluginClassLoaderBridge() {
    }

    /**
     * Registers a Bukkit plugin classloader and its declared dependencies.
     *
     * @param pluginName canonical plugin name
     * @param classLoader plugin classloader
     * @param dependencies declared plugin dependencies
     */
    public void registerPluginClassLoader(String pluginName, ClassLoader classLoader, Set<String> dependencies) {
        Objects.requireNonNull(pluginName, "pluginName cannot be null");
        Objects.requireNonNull(classLoader, "classLoader cannot be null");

        String normalized = pluginName.toLowerCase();
        pluginLoaders.put(normalized, classLoader);
        Set<String> normalizedDeps = new HashSet<>();
        if (dependencies != null) {
            for (String dep : dependencies) {
                if (dep != null && !dep.isBlank()) {
                    normalizedDeps.add(dep.toLowerCase().trim());
                }
            }
        }
        pluginDependencies.put(normalized, Collections.unmodifiableSet(normalizedDeps));
    }

    /**
     * Unregisters a Bukkit plugin classloader.
     *
     * @param pluginName canonical plugin name
     * @return true if removed, false otherwise
     */
    public boolean unregisterPluginClassLoader(String pluginName) {
        Objects.requireNonNull(pluginName, "pluginName cannot be null");
        String normalized = pluginName.toLowerCase();
        pluginDependencies.remove(normalized);
        return pluginLoaders.remove(normalized) != null;
    }

    /**
     * Attempts cross-plugin class resolution with cycle recursion protection and kernel security isolation.
     *
     * @param requestingPlugin the plugin initiating the class lookup
     * @param className fully qualified class name
     * @return resolved Class instance
     * @throws ClassNotFoundException if the class cannot be found in the plugin hierarchy
     * @throws SecurityException if the class attempts to breach protected kernel packages
     */
    public Class<?> findCrossPluginClass(String requestingPlugin, String className) throws ClassNotFoundException {
        Objects.requireNonNull(requestingPlugin, "requestingPlugin cannot be null");
        Objects.requireNonNull(className, "className cannot be null");

        if (isProtectedKernelPackage(className)) {
            throw new SecurityException("Plugin '" + requestingPlugin
                    + "' is not permitted to load internal microkernel class: " + className);
        }

        Set<String> visited = lookupRecursionGuard.get();
        boolean isRoot = false;
        if (visited == null) {
            visited = new HashSet<>();
            lookupRecursionGuard.set(visited);
            isRoot = true;
        }

        String lookupKey = requestingPlugin.toLowerCase() + "!" + className;
        if (!visited.add(lookupKey)) {
            // Cycle detected across plugin dependencies, break recursion gracefully
            throw new ClassNotFoundException("Cyclic class resolution detected for " + className
                    + " via plugin " + requestingPlugin);
        }

        try {
            // 1. Try requesting plugin classloader first
            ClassLoader requesterLoader = pluginLoaders.get(requestingPlugin.toLowerCase());
            if (requesterLoader != null) {
                try {
                    return Class.forName(className, false, requesterLoader);
                } catch (ClassNotFoundException ignored) {
                    // Fallthrough to dependencies
                }
            }

            // 2. Try declared dependencies
            Set<String> deps = pluginDependencies.getOrDefault(requestingPlugin.toLowerCase(), Collections.emptySet());
            for (String dep : deps) {
                ClassLoader depLoader = pluginLoaders.get(dep);
                if (depLoader != null) {
                    try {
                        return Class.forName(className, false, depLoader);
                    } catch (ClassNotFoundException ignored) {
                        // Continue checking other dependencies
                    }
                }
            }

            // 3. Optional global fallback across all plugins
            if (globalFallbackEnabled) {
                for (Map.Entry<String, ClassLoader> entry : pluginLoaders.entrySet()) {
                    if (!entry.getKey().equalsIgnoreCase(requestingPlugin) && !deps.contains(entry.getKey())) {
                        try {
                            return Class.forName(className, false, entry.getValue());
                        } catch (ClassNotFoundException ignored) {
                            // Continue
                        }
                    }
                }
            }

            throw new ClassNotFoundException("Class " + className + " not found across Bukkit plugin hierarchy for "
                    + requestingPlugin);
        } finally {
            visited.remove(lookupKey);
            if (isRoot) {
                lookupRecursionGuard.remove();
            }
        }
    }

    /**
     * Checks whether a class name belongs to a protected internal kernel package.
     *
     * @param className class name to inspect
     * @return true if protected, false otherwise
     */
    public static boolean isProtectedKernelPackage(String className) {
        if (className == null) {
            return false;
        }
        for (int i = 0; i < FORBIDDEN_KERNEL_PREFIXES.length; i++) {
            if (className.startsWith(FORBIDDEN_KERNEL_PREFIXES[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets whether global class resolution fallback across all registered plugins is enabled.
     *
     * @param enabled true to enable global fallback
     */
    public void setGlobalFallbackEnabled(boolean enabled) {
        this.globalFallbackEnabled = enabled;
    }

    /**
     * Checks if global fallback is enabled.
     *
     * @return true if enabled
     */
    public boolean isGlobalFallbackEnabled() {
        return globalFallbackEnabled;
    }

    /**
     * Returns the number of registered plugin classloaders.
     *
     * @return loader count
     */
    public int getRegisteredPluginCount() {
        return pluginLoaders.size();
    }
}
