package dev.vessel.kernel.network;

/**
 * Immutable coalesced network payload containing bundled packet data ready for socket transmission.
 *
 * @param channel     originating network channel index
 * @param packetCount total count of bundled individual packets
 * @param data        raw concatenated byte payload
 * @param byteLength  actual valid payload length in bytes
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record CoalescedPacketPayload(
        int channel,
        int packetCount,
        byte[] data,
        int byteLength
) {
    public boolean isEmpty() {
        return packetCount == 0 || byteLength == 0;
    }
}
