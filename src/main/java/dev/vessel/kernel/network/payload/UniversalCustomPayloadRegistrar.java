package dev.vessel.kernel.network.payload;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Universal custom payload registrar harmonizing NeoForge PayloadRegistrar and Fabric ServerPlayNetworking.
 *
 * <p>Enables mods from all ecosystems to send GUI synchronizations, pipe data, and multiblock
 * updates between client and server without desynchronization or connection drops.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class UniversalCustomPayloadRegistrar {

    private static final UniversalCustomPayloadRegistrar INSTANCE = new UniversalCustomPayloadRegistrar();

    private final Map<String, BiConsumer<Object, byte[]>> payloadReceivers;
    private final PayloadRateLimitAndSanitizer sanitizer;

    /**
     * Constructs a universal payload registrar.
     */
    public UniversalCustomPayloadRegistrar() {
        this.payloadReceivers = new ConcurrentHashMap<>();
        this.sanitizer = new PayloadRateLimitAndSanitizer();
    }

    /**
     * Resolves the global singleton registrar.
     *
     * @return singleton registrar
     */
    public static UniversalCustomPayloadRegistrar getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a custom payload receiver callback for a specific channel/payload id.
     *
     * @param payloadId namespaced payload id (e.g. {@code "neoforge:sync"} or {@code "fabric:network"})
     * @param handler   consumer receiving the player context and raw byte payload
     */
    public void registerReceiver(String payloadId, BiConsumer<Object, byte[]> handler) {
        Objects.requireNonNull(payloadId, "payloadId cannot be null");
        Objects.requireNonNull(handler, "handler cannot be null");
        payloadReceivers.put(payloadId, handler);
    }

    /**
     * Dispatches an incoming custom packet through registered receivers.
     *
     * @param clientIdentifier client IP or identifier for rate-limiting
     * @param player           player instance
     * @param payloadId        target payload identifier
     * @param data             raw byte array
     * @return true if successfully processed
     */
    public boolean dispatchIncomingPayload(String clientIdentifier, Object player, String payloadId, byte[] data) {
        Objects.requireNonNull(payloadId, "payloadId cannot be null");
        if (data == null) {
            data = new byte[0];
        }

        if (!sanitizer.checkAndSanitize(clientIdentifier != null ? clientIdentifier : "unknown", data.length)) {
            return false;
        }

        BiConsumer<Object, byte[]> handler = payloadReceivers.get(payloadId);
        if (handler != null) {
            handler.accept(player, data);
            return true;
        }
        return false;
    }

    /**
     * Checks if a payload channel is registered.
     *
     * @param payloadId channel id
     * @return true if registered
     */
    public boolean hasReceiver(String payloadId) {
        return payloadReceivers.containsKey(payloadId);
    }

    /**
     * Resolves the payload rate limiter and sanitizer.
     *
     * @return sanitizer
     */
    public PayloadRateLimitAndSanitizer getSanitizer() {
        return sanitizer;
    }
}
