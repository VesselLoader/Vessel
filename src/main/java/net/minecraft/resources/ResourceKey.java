package net.minecraft.resources;

import java.util.Objects;

/**
 * Typed Minecraft registry key ({@code net.minecraft.resources.ResourceKey}) combining a parent
 * registry {@link ResourceLocation} with an entry {@link ResourceLocation}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 * @param <T> referenced registry or entry type
 */
public final class ResourceKey<T> {

    private final ResourceLocation registryName;
    private final ResourceLocation location;

    /**
     * Constructs a new {@code ResourceKey} for the specified registry name and entry location.
     *
     * @param registryName parent registry location
     * @param location     target entry or registry location
     */
    public ResourceKey(ResourceLocation registryName, ResourceLocation location) {
        this.registryName = Objects.requireNonNull(registryName, "registryName must not be null");
        this.location = Objects.requireNonNull(location, "location must not be null");
    }

    /**
     * Creates a typed entry {@code ResourceKey} within the given registry key.
     *
     * @param registryKey parent registry key
     * @param location    entry resource location
     * @param <T>         entry type
     * @return new {@code ResourceKey}
     */
    public static <T> ResourceKey<T> create(
            ResourceKey<? extends Object> registryKey, ResourceLocation location) {
        Objects.requireNonNull(registryKey, "registryKey must not be null");
        return new ResourceKey<>(registryKey.location(), location);
    }

    /**
     * SRG alias for {@link #create(ResourceKey, ResourceLocation)}.
     */
    public static <T> ResourceKey<T> m_135785_(
            ResourceKey<? extends Object> registryKey, ResourceLocation location) {
        return create(registryKey, location);
    }

    /**
     * Creates a root registry {@code ResourceKey} for the given registry location.
     *
     * @param registryName registry location (e.g. {@code minecraft:item})
     * @param <T>          registry type
     * @return new root registry {@code ResourceKey}
     */
    public static <T> ResourceKey<T> createRegistryKey(ResourceLocation registryName) {
        return new ResourceKey<>(new ResourceLocation("minecraft", "root"), registryName);
    }

    /**
     * SRG alias for {@link #createRegistryKey(ResourceLocation)}.
     */
    public static <T> ResourceKey<T> m_135788_(ResourceLocation registryName) {
        return createRegistryKey(registryName);
    }

    /**
     * Returns the parent registry {@link ResourceLocation}.
     *
     * @return parent registry location
     */
    public ResourceLocation registry() {
        return registryName;
    }

    /**
     * SRG alias for {@link #registry()}.
     */
    public ResourceLocation m_135780_() {
        return registryName;
    }

    /**
     * Returns the target entry or registry {@link ResourceLocation}.
     *
     * @return target location
     */
    public ResourceLocation location() {
        return location;
    }

    /**
     * SRG alias for {@link #location()}.
     */
    public ResourceLocation m_135782_() {
        return location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResourceKey<?> other)) {
            return false;
        }
        return registryName.equals(other.registryName) && location.equals(other.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registryName, location);
    }

    @Override
    public String toString() {
        return "ResourceKey[" + registryName + " / " + location + "]";
    }
}
