package dev.vessel.kernel.adapter.forge;

import java.util.Objects;

/**
 * Immutable claim representing a planned registration key submission by a mod.
 *
 * @param registryIdentifier namespaced registry name (e.g., "minecraft:item")
 * @param namespacedKey      registered entry key (e.g., "create:wrench")
 * @param ownerModId         namespace identifier of the declaring mod
 * @param declarationSource  source location or class file originating the claim
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record RegistryKeyClaim(
        String registryIdentifier,
        String namespacedKey,
        String ownerModId,
        String declarationSource
) {

    /**
     * Compact constructor validating non-null arguments.
     */
    public RegistryKeyClaim {
        Objects.requireNonNull(registryIdentifier, "registryIdentifier must not be null");
        Objects.requireNonNull(namespacedKey, "namespacedKey must not be null");
        Objects.requireNonNull(ownerModId, "ownerModId must not be null");
        Objects.requireNonNull(declarationSource, "declarationSource must not be null");
    }
}
