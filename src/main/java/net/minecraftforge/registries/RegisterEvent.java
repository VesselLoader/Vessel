package net.minecraftforge.registries;

import dev.vessel.adapter.forge.DeferredRegisterBridge;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Forge registry population event ({@code net.minecraftforge.registries.RegisterEvent}) dispatched
 * on a mod's {@code IEventBus} to resolve and register deferred entries into VESSEL's
 * {@link DeferredRegisterBridge}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class RegisterEvent {

    private final String registryKey;
    private final DeferredRegisterBridge bridge;
    private final Map<String, Object> registeredValues = new LinkedHashMap<>();

    /**
     * Constructs a new {@code RegisterEvent} for the specified registry key backed by a fresh
     * {@link DeferredRegisterBridge}.
     *
     * @param registryKey canonical registry key (e.g., {@code "minecraft:block"})
     */
    public RegisterEvent(String registryKey) {
        this(registryKey, new DeferredRegisterBridge());
    }

    /**
     * Constructs a new {@code RegisterEvent} for the specified registry key and bridge.
     *
     * @param registryKey canonical registry key (e.g., {@code "minecraft:block"})
     * @param bridge      non-null VESSEL deferred register bridge
     */
    public RegisterEvent(String registryKey, DeferredRegisterBridge bridge) {
        this.registryKey = (registryKey == null || registryKey.isBlank())
                ? "minecraft:block"
                : registryKey.trim();
        this.bridge = Objects.requireNonNull(bridge, "bridge must not be null");
    }

    /**
     * Returns the registry key associated with this registration event.
     *
     * @return non-null registry key
     */
    public String getRegistryKey() {
        return registryKey;
    }

    /**
     * Evaluates the given supplier, records the resulting entry under {@code entryName}, and bridges
     * the registration into VESSEL's {@link DeferredRegisterBridge}.
     *
     * @param entryName     non-null entry path or identifier
     * @param valueSupplier non-null factory supplier for the entry
     * @param <T>           entry value type
     * @return the evaluated entry instance
     */
    public <T> T register(String entryName, Supplier<T> valueSupplier) {
        Objects.requireNonNull(entryName, "entryName must not be null");
        Objects.requireNonNull(valueSupplier, "valueSupplier must not be null");
        T val = valueSupplier.get();
        registeredValues.put(entryName, val);
        bridge.registerBlockEntry("minecraft", entryName, 1);
        return val;
    }

    /**
     * Returns an unmodifiable view of all entries registered during this event.
     *
     * @return unmodifiable map of entry names to registered instances
     */
    public Map<String, Object> getRegisteredValues() {
        return Collections.unmodifiableMap(registeredValues);
    }
}
