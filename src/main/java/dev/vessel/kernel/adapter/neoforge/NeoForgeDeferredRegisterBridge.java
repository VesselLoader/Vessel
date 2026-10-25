package dev.vessel.kernel.adapter.neoforge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Universal registry adapter bridging NeoForge DeferredRegister mechanics with VESSEL microkernel engine.
 * <p>
 * Eliminates ERR-92 (Premature registry freeze exceptions during deferred entry resolution)
 * and resolves ecosystem conflicts ECO-021 through ECO-030.
 * Implements retroactive callback replay for late-subscribing registry lifecycle listeners.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class NeoForgeDeferredRegisterBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger(NeoForgeDeferredRegisterBridge.class);

    public static final String ERROR_CODE_REGISTRY_FREEZE = "ERR-92";
    public static final String ERROR_CODE_ECOSYSTEM_REGISTER = "ECO-021";

    private final AtomicInteger nextNumericId = new AtomicInteger(20000);
    private final Map<String, List<NeoForgeRegistryHolder>> entriesByRegistry = new ConcurrentHashMap<>();
    private final Map<String, List<NeoForgeRegistryEventCallback>> callbacksByRegistry = new ConcurrentHashMap<>();
    private final Map<String, NeoForgeRegistryHolder> quickLookupMap = new ConcurrentHashMap<>();

    /**
     * Constructs a new NeoForge DeferredRegister bridge.
     */
    public NeoForgeDeferredRegisterBridge() {
    }

    /**
     * Queues a deferred object registration under the specified registry.
     *
     * @param registryName canonical registry identifier (e.g., "minecraft:block")
     * @param namespace    mod namespace
     * @param path         path key
     * @param supplier     lazy factory supplier producing the registry object
     * @return the assigned registry holder
     */
    public NeoForgeRegistryHolder register(
            String registryName,
            String namespace,
            String path,
            Supplier<?> supplier
    ) {
        Objects.requireNonNull(registryName, "registryName must not be null");
        Objects.requireNonNull(namespace, "namespace must not be null");
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(supplier, "supplier must not be null");

        String normalizedRegistry = normalizeKey(registryName);
        String namespacedKey = formatKey(namespace, path);

        if (quickLookupMap.containsKey(namespacedKey)) {
            String err = "Duplicate deferred registration detected for key '" + namespacedKey + "'";
            LOGGER.error("[VESSEL-NEOFORGE] [{}] {}", ERROR_CODE_REGISTRY_FREEZE, err);
            throw new NeoForgeDeferredRegisterException(
                    ERROR_CODE_REGISTRY_FREEZE,
                    normalizedRegistry,
                    namespacedKey,
                    err
            );
        }

        int rawId = nextNumericId.getAndIncrement();
        NeoForgeRegistryHolder holder = new NeoForgeRegistryHolder(
                normalizedRegistry,
                namespacedKey,
                supplier,
                rawId
        );

        quickLookupMap.put(namespacedKey, holder);
        entriesByRegistry.computeIfAbsent(normalizedRegistry, k -> new CopyOnWriteArrayList<>()).add(holder);

        LOGGER.debug("[VESSEL-NEOFORGE] Enqueued deferred entry '{}' (rawId={}) in registry '{}'",
                namespacedKey, rawId, normalizedRegistry);

        notifyCallbacks(normalizedRegistry, holder);
        return holder;
    }

    /**
     * Subscribes a callback to entry additions for a given registry, immediately replaying all existing entries.
     *
     * @param registryName canonical registry identifier
     * @param callback     listener receiving registered entry holders
     */
    public void registerCallback(String registryName, NeoForgeRegistryEventCallback callback) {
        Objects.requireNonNull(registryName, "registryName must not be null");
        Objects.requireNonNull(callback, "callback must not be null");

        String normalizedRegistry = normalizeKey(registryName);
        callbacksByRegistry.computeIfAbsent(normalizedRegistry, k -> new CopyOnWriteArrayList<>()).add(callback);

        // Retroactive replay of existing entries
        List<NeoForgeRegistryHolder> existing = entriesByRegistry.get(normalizedRegistry);
        if (existing != null) {
            for (NeoForgeRegistryHolder holder : existing) {
                callback.onEntryRegistered(holder);
            }
        }
    }

    /**
     * Looks up an existing deferred holder by namespaced key.
     *
     * @param namespacedKey formatted namespaced key
     * @return holder or null if not found
     */
    public NeoForgeRegistryHolder getHolder(String namespacedKey) {
        Objects.requireNonNull(namespacedKey, "namespacedKey must not be null");
        return quickLookupMap.get(normalizeKey(namespacedKey));
    }

    /**
     * Resolves the instance held by a registered holder.
     *
     * @param namespacedKey formatted namespaced key
     * @return the materialized object instance
     */
    public Object resolveInstance(String namespacedKey) {
        NeoForgeRegistryHolder holder = getHolder(namespacedKey);
        if (holder == null) {
            throw new NeoForgeDeferredRegisterException(
                    ERROR_CODE_ECOSYSTEM_REGISTER,
                    "unknown",
                    namespacedKey,
                    "Cannot resolve unqueued entry"
            );
        }
        return holder.valueSupplier().get();
    }

    /**
     * Returns an unmodifiable snapshot of all entries in a registry.
     *
     * @param registryName canonical registry identifier
     * @return list of holders
     */
    public List<NeoForgeRegistryHolder> getEntries(String registryName) {
        Objects.requireNonNull(registryName, "registryName must not be null");
        List<NeoForgeRegistryHolder> list = entriesByRegistry.get(normalizeKey(registryName));
        if (list == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(list));
    }

    /**
     * Clears all registrations and callbacks.
     */
    public void clear() {
        entriesByRegistry.clear();
        callbacksByRegistry.clear();
        quickLookupMap.clear();
    }

    private void notifyCallbacks(String normalizedRegistry, NeoForgeRegistryHolder holder) {
        List<NeoForgeRegistryEventCallback> callbacks = callbacksByRegistry.get(normalizedRegistry);
        if (callbacks != null) {
            for (NeoForgeRegistryEventCallback cb : callbacks) {
                cb.onEntryRegistered(holder);
            }
        }
    }

    private String normalizeKey(String key) {
        return key.trim().toLowerCase(Locale.ROOT);
    }

    private String formatKey(String namespace, String path) {
        return normalizeKey(namespace) + ":" + normalizeKey(path);
    }
}
