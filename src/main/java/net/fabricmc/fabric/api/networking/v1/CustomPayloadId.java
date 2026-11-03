package net.fabricmc.fabric.api.networking.v1;

import java.util.Objects;

/**
 * Canonical Fabric Networking API typed custom payload identifier
 * ({@code net.fabricmc.fabric.api.networking.v1.CustomPayloadId}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param id  namespaced channel identifier (e.g., {@code "modid:sync"})
 * @param <T> associated {@link CustomPayload} type
 * @version 1.0.0
 */
public record CustomPayloadId<T extends CustomPayload>(String id) {

    /**
     * Compact constructor validating non-null, non-blank channel identifier.
     *
     * @param id namespaced channel identifier
     */
    public CustomPayloadId {
        Objects.requireNonNull(id, "id must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
    }

    /**
     * Creates a typed {@code CustomPayloadId} for the given namespaced identifier.
     *
     * @param id  namespaced channel identifier
     * @param <T> associated {@link CustomPayload} type
     * @return typed payload identifier
     */
    public static <T extends CustomPayload> CustomPayloadId<T> of(String id) {
        return new CustomPayloadId<>(id);
    }
}
