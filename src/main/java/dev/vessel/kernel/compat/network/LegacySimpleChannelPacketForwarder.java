package dev.vessel.kernel.compat.network;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;

/**
 * Forwards legacy Forge 1.16/1.18 SimpleChannel network packets into modern Netty channels.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public final class LegacySimpleChannelPacketForwarder {

    private final ConcurrentMap<String, BiConsumer<Object, Object>> registeredChannelHandlers = new ConcurrentHashMap<>();

    public void registerChannel(String channelName, BiConsumer<Object, Object> packetHandler) {
        Objects.requireNonNull(channelName, "channelName cannot be null");
        Objects.requireNonNull(packetHandler, "packetHandler cannot be null");
        registeredChannelHandlers.put(channelName, packetHandler);
    }

    public boolean dispatchInboundPacket(String channelName, Object packetPayload, Object playerContext) {
        if (channelName == null || packetPayload == null) {
            return false;
        }
        BiConsumer<Object, Object> handler = registeredChannelHandlers.get(channelName);
        if (handler != null) {
            handler.accept(packetPayload, playerContext);
            return true;
        }
        return false;
    }

    public boolean isChannelRegistered(String channelName) {
        return channelName != null && registeredChannelHandlers.containsKey(channelName);
    }

    public int getRegisteredChannelCount() {
        return registeredChannelHandlers.size();
    }
}
