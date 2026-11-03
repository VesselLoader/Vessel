package net.minecraftforge.event.entity;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Forge mod-bus entity attribute registration event
 * ({@code net.minecraftforge.event.entity.EntityAttributeCreationEvent}) allowing mods to bind
 * default attribute maps to custom living entity types.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class EntityAttributeCreationEvent {

    private final Map<String, Map<String, Double>> entityAttributes = new ConcurrentHashMap<>();

    /**
     * Constructs a new {@code EntityAttributeCreationEvent}.
     */
    public EntityAttributeCreationEvent() {
    }

    /**
     * Associates an immutable copy of the given default attribute map with {@code entityTypeKey}.
     *
     * @param entityTypeKey non-null namespaced entity type key (e.g., {@code "mymod:custom_boss"})
     * @param attributes    non-null map of attribute identifiers to base values
     */
    public void put(String entityTypeKey, Map<String, Double> attributes) {
        Objects.requireNonNull(entityTypeKey, "entityTypeKey must not be null");
        Objects.requireNonNull(attributes, "attributes must not be null");
        entityAttributes.put(entityTypeKey.trim(), Map.copyOf(attributes));
    }

    /**
     * Returns {@code true} if default attributes have been registered for {@code entityTypeKey}.
     *
     * @param entityTypeKey entity type key
     * @return {@code true} if attributes are present
     */
    public boolean hasAttributes(String entityTypeKey) {
        if (entityTypeKey == null) {
            return false;
        }
        return entityAttributes.containsKey(entityTypeKey.trim());
    }

    /**
     * Returns the immutable map of default attributes registered for {@code entityTypeKey}, or
     * an empty map if none were registered.
     *
     * @param entityTypeKey entity type key
     * @return immutable map of attribute keys to base values
     */
    public Map<String, Double> getAttributes(String entityTypeKey) {
        if (entityTypeKey == null) {
            return Map.of();
        }
        return entityAttributes.getOrDefault(entityTypeKey.trim(), Map.of());
    }
}
