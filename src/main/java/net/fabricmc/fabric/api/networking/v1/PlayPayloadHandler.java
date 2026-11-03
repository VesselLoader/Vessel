package net.fabricmc.fabric.api.networking.v1;

/**
 * Canonical Fabric Networking API v1 functional handler for incoming play-phase
 * {@link CustomPayload} packets ({@code net.fabricmc.fabric.api.networking.v1.PlayPayloadHandler}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param <T> custom payload type
 * @version 1.0.0
 */
@FunctionalInterface
public interface PlayPayloadHandler<T extends CustomPayload> {

    /**
     * Handles an incoming typed {@link CustomPayload} within the given {@link PlayPayloadContext}.
     *
     * @param payload non-null decoded payload
     * @param context non-null play packet context
     */
    void receive(T payload, PlayPayloadContext context);
}
