package dev.vessel.kernel.compat.era;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

/**
 * Upgrades legacy modded network packets written with raw {@code FriendlyByteBuf} channels
 * (Minecraft 1.12.2 through 1.20.4) into the modern 1.20.5 / 1.21+ {@code CustomPacketPayload}
 * and {@code StreamCodec} wire envelope.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class LegacyModPacketAndCodecUpgrader {

    /**
     * Wraps a legacy mod packet buffer into a canonical 1.21+ {@code CustomPacketPayload} frame
     * prefixed with its normalized {@code namespace:path} channel identifier.
     *
     * @param modId                   the owning mod identifier
     * @param legacyChannelName       the legacy channel string
     * @param rawFriendlyByteBufBytes the raw packet payload bytes
     * @return the framed modern payload bytes
     */
    public byte[] upgradeLegacyPacketPayload(
            String modId,
            String legacyChannelName,
            byte[] rawFriendlyByteBufBytes) {
        Objects.requireNonNull(modId, "modId must not be null");
        Objects.requireNonNull(legacyChannelName, "legacyChannelName must not be null");
        Objects.requireNonNull(
                rawFriendlyByteBufBytes, "rawFriendlyByteBufBytes must not be null");

        String canonicalChannel = normalizeChannelId(modId, legacyChannelName);
        byte[] idBytes = canonicalChannel.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(4 + idBytes.length + rawFriendlyByteBufBytes.length);
        buffer.putInt(idBytes.length);
        buffer.put(idBytes);
        buffer.put(rawFriendlyByteBufBytes);
        return buffer.array();
    }

    /**
     * Extracts the canonical {@code namespace:path} payload identifier from an upgraded modern
     * packet envelope.
     *
     * @param upgradedPayloadBytes the framed modern payload bytes
     * @return the canonical payload identifier string
     */
    public String extractCanonicalPayloadId(byte[] upgradedPayloadBytes) {
        Objects.requireNonNull(upgradedPayloadBytes, "upgradedPayloadBytes must not be null");
        if (upgradedPayloadBytes.length < 4) {
            return "";
        }
        ByteBuffer buffer = ByteBuffer.wrap(upgradedPayloadBytes);
        int idLen = buffer.getInt();
        if (idLen <= 0 || idLen > upgradedPayloadBytes.length - 4) {
            return "";
        }
        byte[] idBytes = new byte[idLen];
        buffer.get(idBytes);
        return new String(idBytes, StandardCharsets.UTF_8);
    }

    private static String normalizeChannelId(String modId, String legacyChannel) {
        String cleanMod = modId.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        String cleanChan = legacyChannel.trim().toLowerCase(Locale.ROOT);
        if (cleanChan.contains(":")) {
            return cleanChan;
        }
        return cleanMod + ":" + cleanChan.replaceAll("[^a-z0-9_./-]", "_");
    }
}
