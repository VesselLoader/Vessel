package dev.vessel.bootstrap.target;

import java.nio.charset.StandardCharsets;

/**
 * Provides the immutable canonical uppercase server brand identifier ({@code "VESSEL"})
 * and encodes the {@code minecraft:brand} custom payload packet buffer.
 *
 * @version 1.0.0
 */
public final class ServerBrandIdentifier {

    private static final String CANONICAL_BRAND = "VESSEL";

    /**
     * Creates a new server brand identifier instance.
     */
    public ServerBrandIdentifier() {
    }

    /**
     * Returns the canonical server brand string. Always returns uppercase {@code "VESSEL"}.
     *
     * @return the canonical brand string {@code "VESSEL"}
     */
    public String getCanonicalServerBrand() {
        return CANONICAL_BRAND;
    }

    /**
     * Encodes the canonical server brand into a Minecraft VarInt-prefixed UTF-8 byte array
     * suitable for the {@code minecraft:brand} custom payload channel.
     *
     * @return VarInt-prefixed UTF-8 payload bytes
     */
    public byte[] encodeBrandCustomPayload() {
        byte[] utf8Bytes = CANONICAL_BRAND.getBytes(StandardCharsets.UTF_8);
        int length = utf8Bytes.length;
        byte[] payload = new byte[1 + length];
        payload[0] = (byte) (length & 0x7F);
        System.arraycopy(utf8Bytes, 0, payload, 1, length);
        return payload;
    }
}
