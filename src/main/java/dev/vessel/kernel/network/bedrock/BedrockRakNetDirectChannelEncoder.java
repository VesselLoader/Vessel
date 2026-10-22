package dev.vessel.kernel.network.bedrock;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * Direct off-heap RakNet channel frame encoder for Bedrock protocol transmission.
 * Writes encapsulated Bedrock packet frames directly into native off-heap DirectByteBuffers,
 * bypassing intermediate Java byte array allocations and trimming end-to-end network latency
 * for Bedrock mobile and console players.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class BedrockRakNetDirectChannelEncoder {

    public static final byte RAKNET_DATAGRAM_ID = (byte) 0x84;

    /**
     * Constructs a BedrockRakNetDirectChannelEncoder.
     */
    public BedrockRakNetDirectChannelEncoder() {
    }

    /**
     * Encodes a RakNet encapsulated frame directly into the destination ByteBuffer.
     *
     * @param dest destination direct or array-backed byte buffer
     * @param header frame encapsulation header
     * @param payload buffer containing payload bytes
     * @return count of bytes written into destination
     */
    public int encodeFrame(ByteBuffer dest, BedrockRakNetFrameHeader header, ByteBuffer payload) {
        Objects.requireNonNull(dest, "dest buffer cannot be null");
        Objects.requireNonNull(header, "header cannot be null");
        Objects.requireNonNull(payload, "payload buffer cannot be null");

        int startPos = dest.position();
        ByteOrder originalOrder = dest.order();
        dest.order(ByteOrder.LITTLE_ENDIAN);

        try {
            // 1. Datagram Header
            dest.put(RAKNET_DATAGRAM_ID);
            writeTriadLE(dest, header.sequenceNumber());

            // 2. Encapsulated Frame Header
            dest.put(header.flags());
            int payloadBits = payload.remaining() * 8;
            dest.order(ByteOrder.BIG_ENDIAN);
            dest.putShort((short) payloadBits);
            dest.order(ByteOrder.LITTLE_ENDIAN);

            if (header.isReliable()) {
                writeTriadLE(dest, header.sequenceNumber());
            }

            if (header.isOrdered()) {
                writeTriadLE(dest, header.orderIndex());
                dest.put(header.orderChannel());
            }

            // 3. Payload copy
            dest.put(payload.slice());

            return dest.position() - startPos;
        } finally {
            dest.order(originalOrder);
        }
    }

    /**
     * Encodes a RakNet frame using a raw byte array payload.
     */
    public int encodeFrame(ByteBuffer dest, BedrockRakNetFrameHeader header, byte[] payload) {
        Objects.requireNonNull(payload, "payload cannot be null");
        return encodeFrame(dest, header, ByteBuffer.wrap(payload));
    }

    /**
     * Decodes a RakNet frame header from the source buffer.
     *
     * @param src source buffer
     * @return decoded frame header
     */
    public BedrockRakNetFrameHeader decodeFrameHeader(ByteBuffer src) {
        Objects.requireNonNull(src, "src buffer cannot be null");
        ByteOrder original = src.order();
        src.order(ByteOrder.LITTLE_ENDIAN);
        try {
            byte datagramId = src.get();
            int seqNum = readTriadLE(src);
            byte flags = src.get();

            src.order(ByteOrder.BIG_ENDIAN);
            int bitLength = src.getShort() & 0xFFFF;
            src.order(ByteOrder.LITTLE_ENDIAN);

            if ((flags & BedrockRakNetFrameHeader.FLAG_RELIABLE) != 0) {
                readTriadLE(src); // Reliable message index
            }

            byte orderChannel = 0;
            int orderIndex = 0;
            if ((flags & BedrockRakNetFrameHeader.FLAG_ORDERED) != 0) {
                orderIndex = readTriadLE(src);
                orderChannel = src.get();
            }

            return new BedrockRakNetFrameHeader(flags, seqNum, orderChannel, orderIndex);
        } finally {
            src.order(original);
        }
    }

    private static void writeTriadLE(ByteBuffer buf, int value) {
        buf.put((byte) (value & 0xFF));
        buf.put((byte) ((value >> 8) & 0xFF));
        buf.put((byte) ((value >> 16) & 0xFF));
    }

    private static int readTriadLE(ByteBuffer buf) {
        int b0 = buf.get() & 0xFF;
        int b1 = buf.get() & 0xFF;
        int b2 = buf.get() & 0xFF;
        return b0 | (b1 << 8) | (b2 << 16);
    }
}
