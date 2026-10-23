package dev.vessel.kernel.network;

import dev.vessel.kernel.vfs.ZeroCopyByteBuffer;

import java.util.Objects;

/**
 * Immutable zero-copy network packet frame for high-throughput packet multiplexing.
 * <p>
 * Binds packet identifier, protocol family, and immutable payload slice without
 * socket buffer allocations.
 * </p>
 *
 * @version 1.0.1
 * @since 1.0.0 (Pass K-6)
 */
public record ZeroCopyPacketFrame(
        int packetId,
        PacketProtocolFamily family,
        ZeroCopyByteBuffer payload,
        long timestampNanos
) {

    public ZeroCopyPacketFrame {
        Objects.requireNonNull(family, "family cannot be null");
        Objects.requireNonNull(payload, "payload cannot be null");
    }

    /**
     * Constructs a packet frame wrapping a raw byte array.
     *
     * @param packetId opcode or packet type id
     * @param family protocol family (Java or Bedrock)
     * @param data payload bytes
     * @return new ZeroCopyPacketFrame
     */
    public static ZeroCopyPacketFrame of(int packetId, PacketProtocolFamily family, byte[] data) {
        Objects.requireNonNull(data, "data cannot be null");
        return new ZeroCopyPacketFrame(packetId, family, ZeroCopyByteBuffer.wrap(data), System.nanoTime());
    }

    /**
     * Constructs a packet frame wrapping a ZeroCopyByteBuffer.
     *
     * @param packetId opcode or packet type id
     * @param family protocol family
     * @param payload zero-copy buffer
     * @return new ZeroCopyPacketFrame
     */
    public static ZeroCopyPacketFrame of(int packetId, PacketProtocolFamily family, ZeroCopyByteBuffer payload) {
        return new ZeroCopyPacketFrame(packetId, family, payload, System.nanoTime());
    }

    /**
     * Returns total length of payload bytes.
     *
     * @return payload length
     */
    public int payloadLength() {
        return payload.length();
    }
}
