package net.minecraftforge.registries;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * Basic in-memory thread-safe implementation of {@link IForgeRegistry}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param <V> value type
 * @version 1.0.0
 */
public final class SimpleForgeRegistry<V> implements IForgeRegistry<V> {

    private final ResourceLocation registryName;
    private final Map<ResourceLocation, V> entries = new ConcurrentHashMap<>();
    private final Map<V, ResourceLocation> reverseEntries = new ConcurrentHashMap<>();

    public SimpleForgeRegistry(ResourceLocation registryName) {
        this.registryName = Objects.requireNonNull(registryName, "registryName must not be null");
    }

    @Override
    public ResourceLocation getRegistryName() {
        return registryName;
    }

    @Override
    public ResourceKey<?> getRegistryKey() {
        return ResourceKey.createRegistryKey(registryName);
    }

    @Override
    public void register(ResourceLocation name, V value) {
        if (name != null && value != null) {
            entries.put(name, value);
            reverseEntries.put(value, name);
        }
    }

    @Override
    public void register(String name, V value) {
        if (name != null && value != null) {
            register(new ResourceLocation(name), value);
        }
    }

    @Override
    public boolean containsKey(ResourceLocation key) {
        return key != null && entries.containsKey(key);
    }

    @Override
    public boolean containsValue(V value) {
        return value != null && reverseEntries.containsKey(value);
    }

    @Override
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @Override
    public V getValue(ResourceLocation key) {
        return key != null ? entries.get(key) : null;
    }

    @Override
    public ResourceLocation getKey(V value) {
        return value != null ? reverseEntries.get(value) : null;
    }

    @Override
    public Set<ResourceLocation> getKeys() {
        return Collections.unmodifiableSet(entries.keySet());
    }

    @Override
    public Collection<V> getValues() {
        return Collections.unmodifiableCollection(entries.values());
    }

    @Override
    public Set<Map.Entry<ResourceKey<V>, V>> getEntries() {
        return Collections.emptySet();
    }

    @Override
    public Iterator<V> iterator() {
        return entries.values().iterator();
    }
}
