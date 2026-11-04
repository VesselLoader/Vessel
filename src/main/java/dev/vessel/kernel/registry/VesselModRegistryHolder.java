package dev.vessel.kernel.registry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe central registry retaining all real mod-registered Item and Block instances
 * across Fabric, Quilt, Forge, and NeoForge mod loaders.
 *
 * <p>Prevents loss of custom behavior, tooltip, NBT, and capabilities caused by placeholder
 * dummy item/block injection.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselModRegistryHolder {

    private static final Map<String, Object> MOD_ITEMS = new ConcurrentHashMap<>();
    private static final Map<String, Object> MOD_BLOCKS = new ConcurrentHashMap<>();

    private VesselModRegistryHolder() {
    }

    /**
     * Stores a real mod item instance under the canonical identifier.
     *
     * @param identifier namespaced identifier (e.g. {@code "create:wrench"})
     * @param instance   concrete item instance
     */
    public static void registerModItem(String identifier, Object instance) {
        Objects.requireNonNull(identifier, "identifier must not be null");
        Objects.requireNonNull(instance, "instance must not be null");
        MOD_ITEMS.put(normalizeKey(identifier), instance);
    }

    /**
     * Stores a real mod block instance under the canonical identifier.
     *
     * @param identifier namespaced identifier (e.g. {@code "create:cogwheel"})
     * @param instance   concrete block instance
     */
    public static void registerModBlock(String identifier, Object instance) {
        Objects.requireNonNull(identifier, "identifier must not be null");
        Objects.requireNonNull(instance, "instance must not be null");
        MOD_BLOCKS.put(normalizeKey(identifier), instance);
    }

    /**
     * Returns the registered mod item instance, or {@code null} if not found.
     *
     * @param identifier namespaced identifier
     * @return registered item instance or {@code null}
     */
    public static Object getModItem(String identifier) {
        if (identifier == null) {
            return null;
        }
        return MOD_ITEMS.get(normalizeKey(identifier));
    }

    /**
     * Returns the registered mod block instance, or {@code null} if not found.
     *
     * @param identifier namespaced identifier
     * @return registered block instance or {@code null}
     */
    public static Object getModBlock(String identifier) {
        if (identifier == null) {
            return null;
        }
        return MOD_BLOCKS.get(normalizeKey(identifier));
    }

    /**
     * Returns an unmodifiable snapshot map of all registered mod items.
     *
     * @return map of namespaced identifiers to real item instances
     */
    public static Map<String, Object> getAllModItems() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(MOD_ITEMS));
    }

    /**
     * Returns an unmodifiable snapshot map of all registered mod blocks.
     *
     * @return map of namespaced identifiers to real block instances
     */
    public static Map<String, Object> getAllModBlocks() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(MOD_BLOCKS));
    }

    /**
     * Checks if a mod item is registered under the given identifier.
     *
     * @param identifier namespaced identifier
     * @return {@code true} if present
     */
    public static boolean hasModItem(String identifier) {
        if (identifier == null) {
            return false;
        }
        return MOD_ITEMS.containsKey(normalizeKey(identifier));
    }

    /**
     * Checks if a mod block is registered under the given identifier.
     *
     * @param identifier namespaced identifier
     * @return {@code true} if present
     */
    public static boolean hasModBlock(String identifier) {
        if (identifier == null) {
            return false;
        }
        return MOD_BLOCKS.containsKey(normalizeKey(identifier));
    }

    /**
     * Clears all registered items and blocks (used primarily for test isolation).
     */
    public static void clear() {
        MOD_ITEMS.clear();
        MOD_BLOCKS.clear();
    }

    private static String normalizeKey(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT);
    }
}
