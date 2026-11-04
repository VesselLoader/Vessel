package net.minecraftforge.registries;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * Standard Forge registry interface ({@code net.minecraftforge.registries.IForgeRegistry})
 * for mods compiled against Forge 1.20.1+.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param <V> value type in registry
 * @version 1.0.0
 */
public interface IForgeRegistry<V> extends Iterable<V> {

    ResourceLocation getRegistryName();

    ResourceKey<?> getRegistryKey();

    void register(ResourceLocation name, V value);

    void register(String name, V value);

    boolean containsKey(ResourceLocation key);

    boolean containsValue(V value);

    boolean isEmpty();

    V getValue(ResourceLocation key);

    ResourceLocation getKey(V value);

    Set<ResourceLocation> getKeys();

    Collection<V> getValues();

    Set<Map.Entry<ResourceKey<V>, V>> getEntries();

    @Override
    Iterator<V> iterator();
}
