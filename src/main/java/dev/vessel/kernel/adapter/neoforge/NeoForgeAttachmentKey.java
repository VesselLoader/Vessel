package dev.vessel.kernel.adapter.neoforge;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Immutable type key descriptor for NeoForge Data Attachment types.
 *
 * @param namespacedKey namespaced key identifier (e.g. "curios:inventory")
 * @param valueClass    class token of the attachment value
 * @param defaultSupplier supplier for default value when attachment is uninitialized
 * @param <T>           attachment value type
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record NeoForgeAttachmentKey<T>(
        String namespacedKey,
        Class<T> valueClass,
        Supplier<T> defaultSupplier
) {

    /**
     * Compact constructor validating non-null arguments.
     */
    public NeoForgeAttachmentKey {
        Objects.requireNonNull(namespacedKey, "namespacedKey must not be null");
        Objects.requireNonNull(valueClass, "valueClass must not be null");
    }
}
