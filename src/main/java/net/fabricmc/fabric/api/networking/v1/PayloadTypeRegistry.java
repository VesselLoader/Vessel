package net.fabricmc.fabric.api.networking.v1;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Canonical Fabric Networking API v1 directional payload codec registry
 * ({@code net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry}).
 *
 * <p>Tracks C2S ({@link #playC2S()}) and S2C ({@link #playS2C()}) custom payload types
 * registered by Fabric and Quilt mods.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class PayloadTypeRegistry {

    private static final PayloadTypeRegistry PLAY_C2S = new PayloadTypeRegistry("PLAY_C2S");
    private static final PayloadTypeRegistry PLAY_S2C = new PayloadTypeRegistry("PLAY_S2C");

    private final String direction;
    private final Map<String, Class<? extends CustomPayload>> registeredTypes = new ConcurrentHashMap<>();

    /**
     * Constructs a directional {@code PayloadTypeRegistry}.
     *
     * @param direction non-null direction label (e.g., {@code "PLAY_C2S"}, {@code "PLAY_S2C"})
     */
    public PayloadTypeRegistry(String direction) {
        this.direction = Objects.requireNonNull(direction, "direction must not be null");
    }

    /**
     * Returns the global client-to-server (C2S) play-phase payload registry.
     *
     * @return C2S payload registry singleton
     */
    public static PayloadTypeRegistry playC2S() {
        return PLAY_C2S;
    }

    /**
     * Returns the global server-to-client (S2C) play-phase payload registry.
     *
     * @return S2C payload registry singleton
     */
    public static PayloadTypeRegistry playS2C() {
        return PLAY_S2C;
    }

    /**
     * Returns the network direction label for this registry.
     *
     * @return direction label
     */
    public String getDirection() {
        return direction;
    }

    /**
     * Registers a custom payload type under its {@link CustomPayloadId}.
     *
     * @param id           non-null payload identifier
     * @param payloadClass non-null payload class
     * @param <T>          payload type
     * @return the registered {@code id}
     */
    public <T extends CustomPayload> CustomPayloadId<T> register(
            CustomPayloadId<T> id, Class<T> payloadClass) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(payloadClass, "payloadClass must not be null");
        registeredTypes.put(id.id(), payloadClass);
        return id;
    }

    /**
     * Returns whether the specified channel identifier is registered in this directional registry.
     *
     * @param id channel identifier string
     * @return {@code true} if registered
     */
    public boolean isRegistered(String id) {
        return id != null && registeredTypes.containsKey(id);
    }

    /**
     * Returns whether the specified {@link CustomPayloadId} is registered in this directional registry.
     *
     * @param id payload identifier
     * @return {@code true} if registered
     */
    public boolean isRegistered(CustomPayloadId<?> id) {
        return id != null && registeredTypes.containsKey(id.id());
    }

    /**
     * Returns an unmodifiable view of all channel IDs registered in this directional registry.
     *
     * @return unmodifiable set of registered channel IDs
     */
    public Set<String> getRegisteredIds() {
        return Collections.unmodifiableSet(registeredTypes.keySet());
    }

    /**
     * Clears all registered payload types in this directional registry.
     */
    public void clear() {
        registeredTypes.clear();
    }
}
