package net.fabricmc.fabric.api.biome.v1;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Canonical Fabric Biome API v1 biome predicate evaluation context
 * ({@code net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class BiomeSelectionContext {

    private final String biomeKey;
    private final Set<String> tags;

    /**
     * Constructs a {@code BiomeSelectionContext} for the specified biome key and tag set.
     *
     * @param biomeKey non-null namespaced biome key
     * @param tags     biome tag identifiers (nullable)
     */
    public BiomeSelectionContext(String biomeKey, Set<String> tags) {
        this.biomeKey = Objects.requireNonNull(biomeKey, "biomeKey must not be null");
        this.tags = tags == null || tags.isEmpty()
                ? Set.of()
                : Collections.unmodifiableSet(new HashSet<>(tags));
    }

    /**
     * Returns the namespaced registry key of the biome being evaluated.
     *
     * @return biome registry key
     */
    public String getBiomeKey() {
        return biomeKey;
    }

    /**
     * Returns whether the biome being evaluated belongs to the specified biome tag.
     *
     * @param tagKey biome tag identifier
     * @return {@code true} if the biome has {@code tagKey}
     */
    public boolean hasTag(String tagKey) {
        return tagKey != null && tags.contains(tagKey);
    }
}
