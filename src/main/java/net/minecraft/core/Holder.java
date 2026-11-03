package net.minecraft.core;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

/**
 * Registry entry reference wrapper ({@code net.minecraft.core.Holder}) pairing a bound value with
 * its {@link ResourceLocation} key and bound {@link TagKey} memberships.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @param <T> held entry type
 */
public final class Holder<T> {

    private final ResourceLocation key;
    private final T value;
    private final Set<TagKey<T>> tags = ConcurrentHashMap.newKeySet();

    /**
     * Constructs a bound {@code Holder} for the specified key and value.
     *
     * @param key   canonical entry location
     * @param value bound entry instance
     */
    public Holder(ResourceLocation key, T value) {
        this.key = Objects.requireNonNull(key, "key must not be null");
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    /**
     * Creates a direct, anonymous holder wrapping {@code value}.
     *
     * @param value bound value
     * @param <T>   held value type
     * @return direct {@code Holder}
     */
    public static <T> Holder<T> direct(T value) {
        return new Holder<>(new ResourceLocation("minecraft", "direct"), value);
    }

    /**
     * Returns the bound value held by this reference.
     *
     * @return bound value
     */
    public T value() {
        return value;
    }

    /**
     * Returns the canonical {@link ResourceLocation} key of this holder.
     *
     * @return resource location key
     */
    public ResourceLocation unwrapKey() {
        return key;
    }

    /**
     * Returns whether this holder has a non-null bound value.
     *
     * @return {@code true} if bound
     */
    public boolean isBound() {
        return value != null;
    }

    /**
     * Checks whether this holder's key matches the specified {@link ResourceLocation}.
     *
     * @param id location to test
     * @return {@code true} if matching
     */
    public boolean is(ResourceLocation id) {
        return key.equals(id);
    }

    /**
     * Checks whether this holder is bound to the specified {@link TagKey}.
     *
     * @param tag tag key to test
     * @return {@code true} if tagged
     */
    public boolean is(TagKey<T> tag) {
        return tag != null && tags.contains(tag);
    }

    /**
     * Associates a {@link TagKey} with this holder.
     *
     * @param tag tag key to bind
     */
    public void bindTag(TagKey<T> tag) {
        if (tag != null) {
            tags.add(tag);
        }
    }

    /**
     * Returns an unmodifiable view of all {@link TagKey}s bound to this holder.
     *
     * @return bound tag set
     */
    public Set<TagKey<T>> tags() {
        return Collections.unmodifiableSet(tags);
    }
}
