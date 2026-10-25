package dev.vessel.kernel.registry;

import java.util.Objects;

/**
 * Immutable descriptor for an individual Data Component codec entry in the fast lookup table.
 *
 * @param componentId primitive integer ID for O(1) direct array indexing
 * @param namespacedKey namespaced identifier (e.g., "minecraft:custom_name")
 * @param valueType binary serialization format type
 * @param defaultValue optional fallback default value
 * @param isTransient whether this component is skipped during long-term world persistence
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record DataComponentCodecDescriptor(
        int componentId,
        String namespacedKey,
        DataComponentValueType valueType,
        Object defaultValue,
        boolean isTransient
) {
    public DataComponentCodecDescriptor {
        if (componentId < 0) {
            throw new IllegalArgumentException("componentId must be non-negative: " + componentId);
        }
        Objects.requireNonNull(namespacedKey, "namespacedKey cannot be null");
        Objects.requireNonNull(valueType, "valueType cannot be null");
    }
}
