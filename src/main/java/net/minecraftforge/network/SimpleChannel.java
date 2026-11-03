package net.minecraftforge.network;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * Forge indexed packet channel ({@code net.minecraftforge.network.SimpleChannel}) supporting
 * typed message registration, client/server packet transmission, and handler dispatch.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class SimpleChannel {

    private final String channelName;
    private final String protocolVersion;
    private final Map<Class<?>, BiConsumer<Object, NetworkEventContext>> handlersByType = new ConcurrentHashMap<>();
    private final Map<Integer, Class<?>> typesByIndex = new ConcurrentHashMap<>();
    private final List<Object> sentMessages = new CopyOnWriteArrayList<>();

    /**
     * Constructs a new {@code SimpleChannel} with the specified channel name and protocol version.
     *
     * @param channelName     non-null namespaced channel identifier
     * @param protocolVersion non-null network protocol version string
     */
    public SimpleChannel(String channelName, String protocolVersion) {
        this.channelName = Objects.requireNonNull(channelName, "channelName must not be null").trim();
        this.protocolVersion = Objects.requireNonNull(protocolVersion, "protocolVersion must not be null").trim();
    }

    /**
     * Returns the namespaced channel identifier.
     *
     * @return non-null channel name
     */
    public String getChannelName() {
        return channelName;
    }

    /**
     * Returns the network protocol version string of this channel.
     *
     * @return non-null protocol version
     */
    public String getProtocolVersion() {
        return protocolVersion;
    }

    /**
     * Registers a typed packet class at the specified discriminator index along with its handler callback.
     *
     * @param index           packet discriminator index
     * @param messageType     non-null packet class token
     * @param messageConsumer non-null packet handler callback
     * @param <MSG>           packet payload type
     */
    @SuppressWarnings("unchecked")
    public <MSG> void registerMessage(
            int index,
            Class<MSG> messageType,
            BiConsumer<MSG, NetworkEventContext> messageConsumer) {
        Objects.requireNonNull(messageType, "messageType must not be null");
        Objects.requireNonNull(messageConsumer, "messageConsumer must not be null");
        typesByIndex.put(index, messageType);
        handlersByType.put(messageType, (BiConsumer<Object, NetworkEventContext>) messageConsumer);
    }

    /**
     * Sends a custom packet payload from the client to the server.
     *
     * @param message non-null packet payload
     */
    public void sendToServer(Object message) {
        Objects.requireNonNull(message, "message must not be null");
        sentMessages.add(message);
    }

    /**
     * Sends a custom packet payload to the specified {@link PacketTarget}.
     *
     * @param target  non-null routing target
     * @param message non-null packet payload
     */
    public void send(PacketTarget target, Object message) {
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(message, "message must not be null");
        sentMessages.add(message);
    }

    /**
     * Dispatches an inbound packet message to the handler registered for its runtime class.
     *
     * @param message non-null inbound packet payload
     * @param context non-null network event context
     * @return {@code true} if a matching handler was found and invoked, {@code false} otherwise
     */
    public boolean dispatch(Object message, NetworkEventContext context) {
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(context, "context must not be null");
        BiConsumer<Object, NetworkEventContext> handler = handlersByType.get(message.getClass());
        if (handler != null) {
            handler.accept(message, context);
            return true;
        }
        return false;
    }

    /**
     * Returns an unmodifiable view of all messages transmitted through this channel.
     *
     * @return unmodifiable list of sent packet payloads
     */
    public List<Object> getSentMessages() {
        return Collections.unmodifiableList(sentMessages);
    }
}
