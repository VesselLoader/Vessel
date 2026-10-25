package dev.vessel.kernel.adapter.forge;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * High-performance network bridge connecting Forge custom payload channels to the VESSEL microkernel pipeline.
 * <p>
 * Eliminates ERR-91 (Forge custom payload routing collisions and channel unregistration disconnects)
 * and addresses ECO-011 through ECO-020.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ForgeChannelPipelineBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeChannelPipelineBridge.class);

    public static final String ERROR_CODE_CHANNEL_ROUTING = "ERR-91";
    public static final String ERROR_CODE_ECOSYSTEM_NETWORK = "ECO-011";

    public static final int DEFAULT_MAX_PAYLOAD_BYTES = 1024 * 1024; // 1 MB payload protection

    private final int maxPayloadSize;
    private final Map<String, ForgeChannelHandler> channelHandlers = new ConcurrentHashMap<>();
    private final Set<String> registeredChannels = new CopyOnWriteArraySet<>();

    /**
     * Constructs a bridge with standard 1MB payload limitation.
     */
    public ForgeChannelPipelineBridge() {
        this(DEFAULT_MAX_PAYLOAD_BYTES);
    }

    /**
     * Constructs a bridge with an explicit maximum payload size.
     *
     * @param maxPayloadSize maximum allowed payload byte count
     */
    public ForgeChannelPipelineBridge(int maxPayloadSize) {
        if (maxPayloadSize <= 0) {
            throw new IllegalArgumentException("maxPayloadSize must be strictly positive");
        }
        this.maxPayloadSize = maxPayloadSize;
    }

    /**
     * Registers a custom payload channel handler.
     *
     * @param channelName namespaced channel identifier (e.g. "curios:sync")
     * @param handler     the subscriber handler
     */
    public void registerChannel(String channelName, ForgeChannelHandler handler) {
        Objects.requireNonNull(channelName, "channelName must not be null");
        Objects.requireNonNull(handler, "handler must not be null");

        String normalized = normalizeChannelName(channelName);
        channelHandlers.put(normalized, handler);
        registeredChannels.add(normalized);
        LOGGER.debug("[VESSEL-FORGE] Registered custom payload channel: {}", normalized);
    }

    /**
     * Unregisters an existing custom payload channel.
     *
     * @param channelName namespaced channel identifier
     * @return true if channel was removed, false otherwise
     */
    public boolean unregisterChannel(String channelName) {
        Objects.requireNonNull(channelName, "channelName must not be null");
        String normalized = normalizeChannelName(channelName);
        boolean removed = channelHandlers.remove(normalized) != null;
        registeredChannels.remove(normalized);
        return removed;
    }

    /**
     * Queries whether a given channel identifier is currently registered.
     *
     * @param channelName namespaced channel identifier
     * @return true if registered
     */
    public boolean isChannelRegistered(String channelName) {
        Objects.requireNonNull(channelName, "channelName must not be null");
        return registeredChannels.contains(normalizeChannelName(channelName));
    }

    /**
     * Routes an incoming or outgoing channel message through the registered pipeline handler.
     *
     * @param message the network payload carrier
     */
    public void routeMessage(ForgeChannelMessage message) {
        Objects.requireNonNull(message, "message must not be null");

        if (message.payloadLength() > maxPayloadSize) {
            String err = String.format("Payload size %d exceeds limit of %d bytes",
                    message.payloadLength(), maxPayloadSize);
            LOGGER.error("[VESSEL-FORGE] [ERR-91] {}", err);
            throw new ForgeChannelPipelineException(ERROR_CODE_CHANNEL_ROUTING, message.channelName(), err);
        }

        String normalized = normalizeChannelName(message.channelName());
        ForgeChannelHandler handler = channelHandlers.get(normalized);

        if (handler == null) {
            String err = "No handler registered for channel '" + normalized + "'";
            LOGGER.warn("[VESSEL-FORGE] [{}] {}", ERROR_CODE_ECOSYSTEM_NETWORK, err);
            throw new ForgeChannelPipelineException(ERROR_CODE_ECOSYSTEM_NETWORK, normalized, err);
        }

        try {
            handler.handlePacket(message);
        } catch (Exception ex) {
            LOGGER.error("[VESSEL-FORGE] Failure processing message on channel '{}': {}",
                    normalized, ex.getMessage(), ex);
            throw new ForgeChannelPipelineException(
                    ERROR_CODE_CHANNEL_ROUTING,
                    normalized,
                    ex.getMessage(),
                    ex
            );
        }
    }

    /**
     * Returns an unmodifiable set of all currently registered channel identifiers.
     *
     * @return set of channels
     */
    public Set<String> getRegisteredChannels() {
        return Collections.unmodifiableSet(registeredChannels);
    }

    /**
     * Clears all registered channel handlers.
     */
    public void clear() {
        channelHandlers.clear();
        registeredChannels.clear();
    }

    private String normalizeChannelName(String raw) {
        String trimmed = raw.trim().toLowerCase(Locale.ROOT);
        if (!trimmed.contains(":")) {
            return "minecraft:" + trimmed;
        }
        return trimmed;
    }
}
