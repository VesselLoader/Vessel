package dev.vessel.kernel.adapter.neoforge;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Immutable holder representing a deferred registry entry queued under NeoForge mechanics.
 *
 * @param registryName  canonical registry name (e.g. "minecraft:block")
 * @param namespacedKey namespaced entry identifier (e.g. "examplemod:custom_ore")
 * @param valueSupplier supplier producing the registered object instance
 * @param rawNumericId  assigned numeric id or -1 if unresolved
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record NeoForgeRegistryHolder(
        String registryName,
        String namespacedKey,
        Supplier<?> valueSupplier,
        int rawNumericId
) {

    /**
     * Compact constructor validating non-null arguments.
     */
    public NeoForgeRegistryHolder {
        Objects.requireNonNull(registryName, "registryName must not be null");
        Objects.requireNonNull(namespacedKey, "namespacedKey must not be null");
        Objects.requireNonNull(valueSupplier, "valueSupplier must not be null");
    }
}
