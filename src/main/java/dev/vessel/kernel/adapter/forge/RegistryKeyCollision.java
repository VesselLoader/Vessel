package dev.vessel.kernel.adapter.forge;

import java.util.Objects;

/**
 * Immutable record capturing a pairwise collision on a registry key between two distinct mods.
 *
 * @param registryIdentifier the registry identifier
 * @param namespacedKey      the conflicting key
 * @param firstModId         first mod claiming the key
 * @param secondModId        second mod claiming the key
 * @param firstSource        source origin of first claim
 * @param secondSource       source origin of second claim
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record RegistryKeyCollision(
        String registryIdentifier,
        String namespacedKey,
        String firstModId,
        String secondModId,
        String firstSource,
        String secondSource
) {

    /**
     * Compact constructor validating non-null arguments.
     */
    public RegistryKeyCollision {
        Objects.requireNonNull(registryIdentifier, "registryIdentifier must not be null");
        Objects.requireNonNull(namespacedKey, "namespacedKey must not be null");
        Objects.requireNonNull(firstModId, "firstModId must not be null");
        Objects.requireNonNull(secondModId, "secondModId must not be null");
        Objects.requireNonNull(firstSource, "firstSource must not be null");
        Objects.requireNonNull(secondSource, "secondSource must not be null");
    }
}
