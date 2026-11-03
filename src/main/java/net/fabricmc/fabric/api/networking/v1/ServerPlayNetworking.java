package net.fabricmc.fabric.api.networking.v1;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Canonical Fabric Networking API v1 server-side play packet router
 * ({@code net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking}).
 *
 * <p>Allows Fabric and Quilt mods to register global C2S packet receivers, query channel
 * availability, and dispatch incoming {@link CustomPayload} packets.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ServerPlayNetworking {

    private static final Map<String, PlayPayloadHandler<?>> GLOBAL_RECEIVERS = new ConcurrentHashMap<>();

    private ServerPlayNetworking() {
    }

    /**
     * Registers a global play-phase packet receiver for the specified {@link CustomPayloadId}.
     *
     * @param id      non-null payload identifier
     * @param handler non-null payload handler callback
     * @param <T>     payload type
     * @return {@code true} if registered
     */
    public static <T extends CustomPayload> boolean registerGlobalReceiver(
            CustomPayloadId<T> id, PlayPayloadHandler<T> handler) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        GLOBAL_RECEIVERS.put(id.id(), handler);
        return true;
    }

    /**
     * Unregisters a global play-phase packet receiver for the specified {@link CustomPayloadId}.
     *
     * @param id non-null payload identifier
     * @return {@code true} if a receiver was removed
     */
    public static boolean unregisterGlobalReceiver(CustomPayloadId<?> id) {
        Objects.requireNonNull(id, "id must not be null");
        return GLOBAL_RECEIVERS.remove(id.id()) != null;
    }

    /**
     * Dispatches an incoming {@link CustomPayload} to the matching registered global receiver.
     *
     * @param payload non-null incoming payload
     * @param context non-null play packet context
     * @param <T>     payload type
     * @return {@code true} if a receiver was registered and invoked, {@code false} otherwise
     */
    @SuppressWarnings("unchecked")
    public static <T extends CustomPayload> boolean dispatchIncomingPayload(
            T payload, PlayPayloadContext context) {
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(context, "context must not be null");
        CustomPayloadId<? extends CustomPayload> payloadId = payload.getId();
        if (payloadId == null) {
            return false;
        }
        PlayPayloadHandler<?> rawHandler = GLOBAL_RECEIVERS.get(payloadId.id());
        if (rawHandler == null) {
            return false;
        }
        ((PlayPayloadHandler<T>) rawHandler).receive(payload, context);
        return true;
    }

    /**
     * Checks whether the specified player connection supports receiving the given {@link CustomPayloadId}.
     *
     * @param playerUuid player UUID
     * @param id         payload identifier
     * @return {@code true} if the channel is registered for S2C or C2S communication
     */
    public static boolean canSend(UUID playerUuid, CustomPayloadId<?> id) {
        return playerUuid != null
                && id != null
                && (PayloadTypeRegistry.playS2C().isRegistered(id)
                        || GLOBAL_RECEIVERS.containsKey(id.id()));
    }

    /**
     * Returns an unmodifiable set of all channel IDs with active global C2S receivers.
     *
     * @return unmodifiable set of registered channel IDs
     */
    public static Set<String> getRegisteredChannelIds() {
        return Collections.unmodifiableSet(GLOBAL_RECEIVERS.keySet());
    }

    /**
     * Clears all registered global play receivers.
     */
    public static void clearReceivers() {
        GLOBAL_RECEIVERS.clear();
    }
}
