package dev.vessel.kernel.compat.fabric;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;

/**
 * Bridges Fabric ServerPlayNetworking API with modern Minecraft custom packet channels and codecs.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public final class FabricNetworkingApiBridge {

    private final ConcurrentMap<String, BiConsumer<Object, Object>> globalPacketReceivers = new ConcurrentHashMap<>();

    public void registerGlobalReceiver(String channelId, BiConsumer<Object, Object> packetHandler) {
        Objects.requireNonNull(channelId, "channelId cannot be null");
        Objects.requireNonNull(packetHandler, "packetHandler cannot be null");
        globalPacketReceivers.put(channelId, packetHandler);
    }

    public boolean handleIncomingFabricPacket(String channelId, Object serverPlayer, Object packetBuffer) {
        if (channelId == null || serverPlayer == null || packetBuffer == null) {
            return false;
        }
        BiConsumer<Object, Object> receiver = globalPacketReceivers.get(channelId);
        if (receiver != null) {
            receiver.accept(serverPlayer, packetBuffer);
            return true;
        }
        return false;
    }

    public boolean isChannelSubscribed(String channelId) {
        return channelId != null && globalPacketReceivers.containsKey(channelId);
    }

    public Set<String> getSubscribedChannels() {
        return Set.copyOf(globalPacketReceivers.keySet());
    }

    public void unregisterReceiver(String channelId) {
        if (channelId != null) {
            globalPacketReceivers.remove(channelId);
        }
    }
}
