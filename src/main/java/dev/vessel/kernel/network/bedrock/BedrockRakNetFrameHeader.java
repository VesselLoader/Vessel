package dev.vessel.kernel.network.bedrock;

/**
 * Immutable RakNet encapsulation frame header for direct Bedrock UDP packet streams.
 *
 * @param flags packet reliability and fragmentation bit flags
 * @param sequenceNumber 24-bit sequential datagram packet number
 * @param orderChannel ordering channel index (0..31)
 * @param orderIndex sequence counter for ordered delivery
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record BedrockRakNetFrameHeader(
        byte flags,
        int sequenceNumber,
        byte orderChannel,
        int orderIndex
) {
    public static final byte FLAG_RELIABLE = (byte) 0x40;
    public static final byte FLAG_ORDERED  = (byte) 0x20;
    public static final byte FLAG_SPLIT    = (byte) 0x10;

    /**
     * Checks if this frame requires acknowledgment / reliable delivery.
     */
    public boolean isReliable() {
        return (flags & FLAG_RELIABLE) != 0;
    }

    /**
     * Checks if this frame enforces in-order delivery.
     */
    public boolean isOrdered() {
        return (flags & FLAG_ORDERED) != 0;
    }

    /**
     * Checks if this frame is fragmented across multiple datagrams.
     */
    public boolean isSplit() {
        return (flags & FLAG_SPLIT) != 0;
    }
}
