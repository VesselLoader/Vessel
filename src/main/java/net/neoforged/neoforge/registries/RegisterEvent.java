package net.neoforged.neoforge.registries;

import dev.vessel.adapter.forge.DeferredRegisterBridge;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * NeoForge registry event ({@code net.neoforged.neoforge.registries.RegisterEvent}) fired on the
 * mod event bus to bind queued {@link DeferredRegister} entries and direct registration callbacks.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public final class RegisterEvent {

    private final String registryKey;
    private final DeferredRegisterBridge bridge;
    private final Map<String, Object> registeredValues = new LinkedHashMap<>();

    /**
     * Constructs a {@code RegisterEvent} for the given registry key and underlying bridge.
     *
     * @param registryKey canonical registry key (e.g. {@code "minecraft:block"})
     * @param bridge      VESSEL {@link DeferredRegisterBridge} instance
     */
    public RegisterEvent(String registryKey, DeferredRegisterBridge bridge) {
        this.registryKey = Objects.requireNonNull(registryKey, "registryKey must not be null");
        this.bridge = Objects.requireNonNull(bridge, "bridge must not be null");
    }

    /**
     * Constructs a {@code RegisterEvent} for the given registry key backed by a new bridge.
     *
     * @param registryKey canonical registry key
     */
    public RegisterEvent(String registryKey) {
        this(registryKey, new DeferredRegisterBridge());
    }

    /**
     * Returns the target registry key associated with this registration event.
     *
     * @return canonical registry key
     */
    public String getRegistryKey() {
        return registryKey;
    }

    /**
     * Evaluates {@code valueSupplier}, records the entry in the underlying VESSEL bridge, and
     * stores the resolved object in this event's registered value map and {@link dev.vessel.kernel.registry.VesselModRegistryHolder}.
     *
     * @param entryName     entry identifier ({@code namespace:path} or {@code path})
     * @param valueSupplier supplier producing the entry value
     * @param <T>           entry type
     * @return the resolved entry instance
     */
    public <T> T register(String entryName, Supplier<T> valueSupplier) {
        Objects.requireNonNull(entryName, "entryName must not be null");
        Objects.requireNonNull(valueSupplier, "valueSupplier must not be null");
        T value = valueSupplier.get();
        int colonIdx = entryName.indexOf(':');
        String namespace = colonIdx >= 0 ? entryName.substring(0, colonIdx) : "minecraft";
        String path = colonIdx >= 0 ? entryName.substring(colonIdx + 1) : entryName;
        String fullKey = namespace + ":" + path;
        boolean isItem = registryKey.toLowerCase(Locale.ROOT).contains("item");
        if (isItem) {
            bridge.registerItemEntry(namespace, path, Collections.emptyMap());
            if (value != null) {
                dev.vessel.kernel.registry.VesselModRegistryHolder.registerModItem(fullKey, value);
            }
        } else {
            bridge.registerBlockEntry(namespace, path, 1);
            if (value != null) {
                dev.vessel.kernel.registry.VesselModRegistryHolder.registerModBlock(fullKey, value);
            }
        }
        registeredValues.put(entryName, value);
        return value;
    }

    /**
     * Returns an unmodifiable view of all values registered through this event.
     *
     * @return unmodifiable map of entry names to registered instances
     */
    public Map<String, Object> getRegisteredValues() {
        return Collections.unmodifiableMap(registeredValues);
    }
}
