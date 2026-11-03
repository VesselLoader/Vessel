package dev.vessel.bootstrap.classloading;

import java.util.HashSet;
import java.util.Set;

/**
 * Bidirectional classloader bridge implementing ADR-0001 ({@code VES-CLASS-001}) between the
 * Tier-1 Game ClassLoader ({@code TierOneGameTransformingClassLoader}) and the Tier-2 Mod
 * ClassLoader ({@code LiveJarBytecodeTransformingClassLoader}) with thread-local reentrancy
 * protection so cross-tier class resolution never deadlocks or enters infinite recursion.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class UnifiedGameAndModClassLoaderBridge {

    private volatile ClassLoader tierOneGameClassLoader;
    private volatile ClassLoader tierTwoModClassLoader;
    private final ThreadLocal<Set<String>> activeResolutionSet = new ThreadLocal<>();

    public void bindTierOneGameClassLoader(ClassLoader gameClassLoader) {
        this.tierOneGameClassLoader = gameClassLoader;
    }

    public void bindTierTwoModClassLoader(ClassLoader modClassLoader) {
        this.tierTwoModClassLoader = modClassLoader;
    }

    public ClassLoader getTierOneGameClassLoader() {
        return tierOneGameClassLoader;
    }

    public ClassLoader getTierTwoModClassLoader() {
        return tierTwoModClassLoader;
    }

    public boolean isFullyBridged() {
        return tierOneGameClassLoader != null && tierTwoModClassLoader != null;
    }

    public Class<?> tryLoadFromModClassLoader(String className) {
        if (className == null || className.isBlank() || tierTwoModClassLoader == null) {
            return null;
        }
        Set<String> set = getOrCreateResolutionSet();
        String key = "MOD:" + className;
        if (!set.add(key)) {
            return null;
        }
        try {
            return Class.forName(className, false, tierTwoModClassLoader);
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        } finally {
            set.remove(key);
        }
    }

    public Class<?> tryLoadFromGameClassLoader(String className) {
        if (className == null || className.isBlank() || tierOneGameClassLoader == null) {
            return null;
        }
        Set<String> set = getOrCreateResolutionSet();
        String key = "GAME:" + className;
        if (!set.add(key)) {
            return null;
        }
        try {
            return Class.forName(className, false, tierOneGameClassLoader);
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        } finally {
            set.remove(key);
        }
    }

    private Set<String> getOrCreateResolutionSet() {
        Set<String> set = activeResolutionSet.get();
        if (set == null) {
            set = new HashSet<>();
            activeResolutionSet.set(set);
        }
        return set;
    }
}
