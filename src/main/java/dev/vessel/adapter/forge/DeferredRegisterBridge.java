package dev.vessel.adapter.forge;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Bridges Forge and NeoForge {@code DeferredRegister} queues into the VESSEL unified
 * registry, supporting retroactive callback replay for early-registered entries
 * per Lessons L26 and L27.
 *
 * @version 1.0.0
 */
public final class DeferredRegisterBridge {

    private static final String BLOCK_REGISTRY = "minecraft:block";
    private static final String ITEM_REGISTRY = "minecraft:item";

    private final AtomicInteger nextRawId = new AtomicInteger(1000);
    private final Map<String, Integer> blockStateCounts = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> itemDefaultComponents = new ConcurrentHashMap<>();
    private final Map<String, List<String>> registeredKeysByRegistry = new ConcurrentHashMap<>();
    private final Map<String, List<Consumer<String>>> entryCallbacks = new ConcurrentHashMap<>();

    /**
     * Creates a new DeferredRegister bridge.
     */
    public DeferredRegisterBridge() {
    }

    /**
     * Registers a modded block entry and notifies any active {@code minecraft:block} callbacks.
     *
     * @param namespace                   mod namespace
     * @param path                        block path identifier
     * @param defaultStatePropertiesCount number of block state property permutations
     * @return assigned numeric raw block ID
     */
    public int registerBlockEntry(String namespace, String path, int defaultStatePropertiesCount) {
        String key = formatNamespacedKey(namespace, path);
        int rawId = nextRawId.getAndIncrement();
        blockStateCounts.put(key, Math.max(1, defaultStatePropertiesCount));
        recordAndNotify(BLOCK_REGISTRY, key);
        return rawId;
    }

    /**
     * Registers a modded item entry with its default Data Components (1.20.5+ compatibility)
     * and notifies any active {@code minecraft:item} callbacks.
     *
     * @param namespace             mod namespace
     * @param path                  item path identifier
     * @param defaultDataComponents default item data component map
     * @return assigned numeric raw item ID
     */
    public int registerItemEntry(
            String namespace, String path, Map<String, Object> defaultDataComponents) {
        Objects.requireNonNull(defaultDataComponents, "defaultDataComponents must not be null");
        String key = formatNamespacedKey(namespace, path);
        int rawId = nextRawId.getAndIncrement();
        itemDefaultComponents.put(
                key, Collections.unmodifiableMap(new LinkedHashMap<>(defaultDataComponents)));
        recordAndNotify(ITEM_REGISTRY, key);
        return rawId;
    }

    /**
     * Registers a callback invoked whenever an entry is added to the specified registry,
     * immediately replaying all previously registered entries for that registry (Lesson L26).
     *
     * @param registryName canonical registry identifier (e.g., {@code "minecraft:block"})
     * @param callback     consumer receiving the namespaced entry key
     */
    public void registerEntryAddedCallback(String registryName, Consumer<String> callback) {
        Objects.requireNonNull(registryName, "registryName must not be null");
        Objects.requireNonNull(callback, "callback must not be null");
        String normalizedRegistry = registryName.trim().toLowerCase(Locale.ROOT);
        List<Consumer<String>> callbacks =
                entryCallbacks.computeIfAbsent(normalizedRegistry, k -> new CopyOnWriteArrayList<>());
        callbacks.add(callback);

        List<String> existingEntries = registeredKeysByRegistry.get(normalizedRegistry);
        if (existingEntries != null) {
            for (String existingKey : existingEntries) {
                callback.accept(existingKey);
            }
        }
    }

    private void recordAndNotify(String registryName, String entryKey) {
        List<String> keys =
                registeredKeysByRegistry.computeIfAbsent(
                        registryName, k -> new CopyOnWriteArrayList<>());
        keys.add(entryKey);
        List<Consumer<String>> callbacks = entryCallbacks.get(registryName);
        if (callbacks != null) {
            for (Consumer<String> callback : callbacks) {
                callback.accept(entryKey);
            }
        }
    }

    private static String formatNamespacedKey(String namespace, String path) {
        Objects.requireNonNull(namespace, "namespace must not be null");
        Objects.requireNonNull(path, "path must not be null");
        return namespace.trim().toLowerCase(Locale.ROOT)
                + ":"
                + path.trim().toLowerCase(Locale.ROOT);
    }
}
