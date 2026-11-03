package net.minecraft.tags;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * Typed Minecraft tag key ({@code net.minecraft.tags.TagKey}) associating a tag
 * {@link ResourceLocation} with its target registry {@link ResourceLocation}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @param registry target registry identifier (e.g., {@code minecraft:block})
 * @param location tag identifier (e.g., {@code minecraft:mineable/pickaxe})
 * @param <T>      tagged registry entry type
 */
public record TagKey<T>(ResourceLocation registry, ResourceLocation location) {

    /**
     * Compact constructor validating non-null registry and tag locations.
     *
     * @param registry target registry identifier
     * @param location tag identifier
     */
    public TagKey {
        Objects.requireNonNull(registry, "registry must not be null");
        Objects.requireNonNull(location, "location must not be null");
    }

    /**
     * Creates a new {@code TagKey} for the specified registry and tag location.
     *
     * @param registry target registry identifier
     * @param location tag identifier
     * @param <T>      tagged registry entry type
     * @return new {@code TagKey}
     */
    public static <T> TagKey<T> create(ResourceLocation registry, ResourceLocation location) {
        return new TagKey<>(registry, location);
    }

    /**
     * Checks whether this tag key belongs to the specified target registry.
     *
     * @param targetRegistry registry identifier to compare
     * @return {@code true} if this tag key targets {@code targetRegistry}
     */
    public boolean isFor(ResourceLocation targetRegistry) {
        return registry.equals(targetRegistry);
    }
}
