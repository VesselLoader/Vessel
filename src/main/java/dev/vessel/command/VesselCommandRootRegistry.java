package dev.vessel.command;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic thread-safe registry tracking all active command root literals across kernel,
 * Bukkit plugins, and Forge/NeoForge mods to eliminate static command hardcoding.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselCommandRootRegistry {

    private static final Set<String> KERNEL_ROOTS = ConcurrentHashMap.newKeySet();
    private static final Set<String> BUKKIT_ROOTS = ConcurrentHashMap.newKeySet();
    private static final Set<String> MOD_ROOTS = ConcurrentHashMap.newKeySet();

    static {
        // Default Sovereign Kernel command suite
        registerKernelRoot("vessel");
        registerKernelRoot("geyser");
        registerKernelRoot("floodgate");
        registerKernelRoot("fwhitelist");
        registerKernelRoot("spark");
        registerKernelRoot("restart");

        // Built-in Bukkit / Essentials bridge commands
        registerBukkitRoot("sethome");
        registerBukkitRoot("home");
        registerBukkitRoot("delhome");
        registerBukkitRoot("spawn");
        registerBukkitRoot("heal");
        registerBukkitRoot("feed");
        registerBukkitRoot("fly");
        registerBukkitRoot("esethome");
        registerBukkitRoot("createhome");
    }

    private VesselCommandRootRegistry() {
    }

    public static void registerKernelRoot(String name) {
        if (name != null && !name.isBlank()) {
            KERNEL_ROOTS.add(name.trim().toLowerCase(Locale.ROOT));
        }
    }

    public static void registerBukkitRoot(String name) {
        if (name != null && !name.isBlank()) {
            BUKKIT_ROOTS.add(name.trim().toLowerCase(Locale.ROOT));
        }
    }

    public static void registerModRoot(String name) {
        if (name != null && !name.isBlank()) {
            MOD_ROOTS.add(name.trim().toLowerCase(Locale.ROOT));
        }
    }

    public static boolean isKernelRoot(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return KERNEL_ROOTS.contains(name.trim().toLowerCase(Locale.ROOT));
    }

    public static boolean isBukkitRoot(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return BUKKIT_ROOTS.contains(name.trim().toLowerCase(Locale.ROOT));
    }

    public static boolean isModRoot(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return MOD_ROOTS.contains(name.trim().toLowerCase(Locale.ROOT));
    }

    public static Set<String> getKernelRoots() {
        return Collections.unmodifiableSet(KERNEL_ROOTS);
    }

    public static Set<String> getBukkitRoots() {
        return Collections.unmodifiableSet(BUKKIT_ROOTS);
    }

    public static Set<String> getModRoots() {
        return Collections.unmodifiableSet(MOD_ROOTS);
    }
}
