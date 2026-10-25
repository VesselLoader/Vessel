package dev.vessel.kernel.network.bridge;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Intercepts outbound and inbound Netty packet channels per player session, translating or
 * filtering custom mod channels for Bedrock clients and enforcing per-player inbound packet rate
 * limits to prevent packet-flood exploits.
 *
 * @version 1.0.0
 */
public final class VesselNettyChannelInterceptor {

    private static final int MAX_INBOUND_PAYLOAD_BYTES = 2_097_152;

    private final Map<UUID, AtomicInteger> inboundPacketCounters = new ConcurrentHashMap<>();

    /**
     * Intercepts an outbound custom payload packet before transmission on the player's Netty
     * pipeline.
     *
     * @param playerUuid      the recipient player's UUID
     * @param packetChannel   the namespaced packet channel identifier
     * @param rawPayload      the raw outbound payload bytes
     * @param isBedrockClient whether the player is connected via Bedrock RakNet
     * @return the outbound payload bytes (framed for Bedrock if applicable, or empty if filtered)
     */
    public byte[] interceptOutboundPacket(
            UUID playerUuid,
            String packetChannel,
            byte[] rawPayload,
            boolean isBedrockClient) {
        Objects.requireNonNull(playerUuid, "playerUuid must not be null");
        Objects.requireNonNull(packetChannel, "packetChannel must not be null");
        Objects.requireNonNull(rawPayload, "rawPayload must not be null");
        if (!isBedrockClient) {
            return Arrays.copyOf(rawPayload, rawPayload.length);
        }
        if (packetChannel.startsWith("minecraft:") || packetChannel.startsWith("vessel:")) {
            byte[] prefix = ("bedrock:" + packetChannel + ":").getBytes(StandardCharsets.UTF_8);
            byte[] framed = new byte[prefix.length + rawPayload.length];
            System.arraycopy(prefix, 0, framed, 0, prefix.length);
            System.arraycopy(rawPayload, 0, framed, prefix.length, rawPayload.length);
            return framed;
        }
        return new byte[0];
    }

    /**
     * Intercepts an inbound packet from a player session and enforces payload size and per-second
     * packet rate limits.
     *
     * @param playerUuid          the sender player's UUID
     * @param packetChannel       the namespaced packet channel identifier
     * @param rawPayload          the raw inbound payload bytes
     * @param maxPacketsPerSecond the maximum allowed packets per second for the player
     * @return {@code true} if the packet is accepted, or {@code false} if rejected by the firewall
     */
    public boolean interceptInboundPacket(
            UUID playerUuid,
            String packetChannel,
            byte[] rawPayload,
            int maxPacketsPerSecond) {
        Objects.requireNonNull(playerUuid, "playerUuid must not be null");
        Objects.requireNonNull(packetChannel, "packetChannel must not be null");
        Objects.requireNonNull(rawPayload, "rawPayload must not be null");
        if (rawPayload.length > MAX_INBOUND_PAYLOAD_BYTES || maxPacketsPerSecond <= 0) {
            return false;
        }
        int count = inboundPacketCounters
                .computeIfAbsent(playerUuid, ignored -> new AtomicInteger(0))
                .incrementAndGet();
        return count <= maxPacketsPerSecond;
    }

    /**
     * Resets per-second inbound packet counters across all connected players.
     */
    public void resetSecondWindowCounters() {
        inboundPacketCounters.clear();
    }
}
