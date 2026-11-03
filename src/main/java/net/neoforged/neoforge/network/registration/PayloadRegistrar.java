package net.neoforged.neoforge.network.registration;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * NeoForge network payload registrar
 * ({@code net.neoforged.neoforge.network.registration.PayloadRegistrar}) used during
 * {@code RegisterPayloadHandlersEvent} to bind serverbound, clientbound, and bidirectional
 * custom payload handlers.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class PayloadRegistrar {

    private final String version;
    private final Map<String, BiConsumer<Object, IPayloadContext>> handlers;

    /**
     * Constructs a new {@code PayloadRegistrar} with the specified protocol version.
     *
     * @param version protocol version string
     */
    public PayloadRegistrar(String version) {
        this(version, new ConcurrentHashMap<>());
    }

    private PayloadRegistrar(
            String version,
            Map<String, BiConsumer<Object, IPayloadContext>> handlers) {
        this.version = Objects.requireNonNull(version, "version must not be null");
        this.handlers = Objects.requireNonNull(handlers, "handlers must not be null");
    }

    /**
     * Returns the protocol version configured on this registrar.
     *
     * @return protocol version string
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns a derived {@code PayloadRegistrar} with a new version string sharing the same
     * underlying payload handler registry.
     *
     * @param newVersion updated protocol version string
     * @return derived {@code PayloadRegistrar}
     */
    public PayloadRegistrar versioned(String newVersion) {
        return new PayloadRegistrar(newVersion, this.handlers);
    }

    /**
     * Registers a serverbound play-phase custom payload handler.
     *
     * @param payloadId canonical payload identifier
     * @param handler   payload handler callback
     * @return {@code this} registrar for fluent chaining
     */
    public PayloadRegistrar playToServer(
            String payloadId, BiConsumer<Object, IPayloadContext> handler) {
        Objects.requireNonNull(payloadId, "payloadId must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        handlers.put(payloadId, handler);
        return this;
    }

    /**
     * Registers a clientbound play-phase custom payload handler.
     *
     * @param payloadId canonical payload identifier
     * @param handler   payload handler callback
     * @return {@code this} registrar for fluent chaining
     */
    public PayloadRegistrar playToClient(
            String payloadId, BiConsumer<Object, IPayloadContext> handler) {
        Objects.requireNonNull(payloadId, "payloadId must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        handlers.put(payloadId, handler);
        return this;
    }

    /**
     * Registers a bidirectional play-phase custom payload handler.
     *
     * @param payloadId canonical payload identifier
     * @param handler   payload handler callback
     * @return {@code this} registrar for fluent chaining
     */
    public PayloadRegistrar playBidirectional(
            String payloadId, BiConsumer<Object, IPayloadContext> handler) {
        Objects.requireNonNull(payloadId, "payloadId must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        handlers.put(payloadId, handler);
        return this;
    }

    /**
     * Checks whether a handler is registered for the given payload identifier.
     *
     * @param payloadId canonical payload identifier
     * @return {@code true} if registered
     */
    public boolean isRegistered(String payloadId) {
        if (payloadId == null) {
            return false;
        }
        return handlers.containsKey(payloadId);
    }

    /**
     * Dispatches a decoded payload object to its registered handler under {@code payloadId}.
     *
     * @param payloadId canonical payload identifier
     * @param payload   decoded payload instance
     * @param context   active payload execution context
     * @return {@code true} if a matching handler was found and invoked
     */
    public boolean dispatch(String payloadId, Object payload, IPayloadContext context) {
        Objects.requireNonNull(payloadId, "payloadId must not be null");
        Objects.requireNonNull(context, "context must not be null");
        BiConsumer<Object, IPayloadContext> handler = handlers.get(payloadId);
        if (handler == null) {
            return false;
        }
        handler.accept(payload, context);
        return true;
    }
}
