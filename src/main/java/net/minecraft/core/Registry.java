package net.minecraft.core;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * Canonical Minecraft key-value registry ({@code net.minecraft.core.Registry}) mapping
 * {@link ResourceLocation} identifiers to typed game objects and {@link Holder} references.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 * @param <T> registered entry type
 */
public class Registry<T> {

    private final ResourceKey<? extends Registry<T>> key;
    private final Map<ResourceLocation, T> byId = new ConcurrentHashMap<>();
    private final Map<T, ResourceLocation> byValue = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, Holder<T>> holders = new ConcurrentHashMap<>();

    /**
     * Constructs a new {@code Registry} identified by the given {@link ResourceKey}.
     *
     * @param key canonical registry key
     */
    public Registry(ResourceKey<? extends Registry<T>> key) {
        this.key = Objects.requireNonNull(key, "key must not be null");
    }

    /**
     * Static helper that registers {@code value} into {@code registry} under {@code id}.
     *
     * @param registry target registry
     * @param id       entry location
     * @param value    entry instance
     * @param <V>      registry base type
     * @param <T>      concrete entry subtype
     * @return registered {@code value}
     */
    public static <V, T extends V> T register(
            Registry<V> registry, ResourceLocation id, T value) {
        Objects.requireNonNull(registry, "registry must not be null");
        registry.registerEntry(id, value);
        return value;
    }

    /**
     * Static helper that parses {@code id} and registers {@code value} into {@code registry}.
     *
     * @param registry target registry
     * @param id       entry location string
     * @param value    entry instance
     * @param <V>      registry base type
     * @param <T>      concrete entry subtype
     * @return registered {@code value}
     */
    public static <V, T extends V> T register(Registry<V> registry, String id, T value) {
        return register(registry, ResourceLocation.parse(id), value);
    }

    /**
     * Registers an entry in this registry and creates its corresponding {@link Holder}.
     *
     * @param id    non-null entry location
     * @param value non-null entry value
     * @return registered {@code value}
     */
    public T registerEntry(ResourceLocation id, T value) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(value, "value must not be null");
        byId.put(id, value);
        byValue.put(value, id);
        holders.put(id, new Holder<>(id, value));
        if (key != null && key.location() != null) {
            String path = key.location().getPath();
            if ("item".equals(path)) {
                dev.vessel.kernel.registry.VesselModRegistryHolder.registerModItem(id.toString(), value);
            } else if ("block".equals(path)) {
                dev.vessel.kernel.registry.VesselModRegistryHolder.registerModBlock(id.toString(), value);
            }
        }
        return value;
    }

    /**
     * Retrieves the entry registered under {@code id}, or {@code null} if not present.
     *
     * @param id entry location
     * @return registered entry or {@code null}
     */
    public T get(ResourceLocation id) {
        if (id == null) {
            return null;
        }
        return byId.get(id);
    }

    /**
     * Retrieves an {@link Optional} containing the entry registered under {@code id}.
     *
     * @param id entry location
     * @return optional entry
     */
    public Optional<T> getOptional(ResourceLocation id) {
        return Optional.ofNullable(get(id));
    }

    /**
     * Looks up the {@link ResourceLocation} key for a registered value, or {@code null} if
     * unregistered.
     *
     * @param value entry instance
     * @return entry location or {@code null}
     */
    public ResourceLocation getKey(T value) {
        if (value == null) {
            return null;
        }
        return byValue.get(value);
    }

    /**
     * Retrieves the {@link Holder} for the given entry location, if registered.
     *
     * @param id entry location
     * @return optional holder
     */
    public Optional<Holder<T>> getHolder(ResourceLocation id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(holders.get(id));
    }

    /**
     * Checks whether an entry is registered under {@code id}.
     *
     * @param id entry location
     * @return {@code true} if present
     */
    public boolean containsKey(ResourceLocation id) {
        return id != null && byId.containsKey(id);
    }

    /**
     * Returns an unmodifiable view of all registered keys in this registry.
     *
     * @return key set
     */
    public Set<ResourceLocation> keySet() {
        return Collections.unmodifiableSet(byId.keySet());
    }

    /**
     * Returns the number of entries in this registry.
     *
     * @return entry count
     */
    public int size() {
        return byId.size();
    }

    /**
     * Returns the canonical {@link ResourceKey} identifying this registry.
     *
     * @return registry key
     */
    public ResourceKey<? extends Registry<T>> key() {
        return key;
    }
}
