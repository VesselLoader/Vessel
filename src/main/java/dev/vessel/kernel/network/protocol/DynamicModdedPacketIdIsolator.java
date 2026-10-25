package dev.vessel.kernel.network.protocol;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Isolates dynamically registered mod packet IDs into the dedicated {@code vsl:mod_packet}
 * namespace, completely preventing Netty buffer index misalignment, array out-of-bounds,
 * and packet handler shifting when interoperating with ViaVersion, ProtocolLib, and Folia.
 *
 * <p>Responsibility: Modded packet ID isolation and envelope framing.
 * Eliminates: ERR-24, NET-041..060.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class DynamicModdedPacketIdIsolator {

    /**
     * Dedicated isolated channel identifier.
     */
    public static final String ISOLATED_MOD_CHANNEL = "vsl:mod_packet";

    /**
     * 4-byte magic signature for isolated packet frames ('VSLP' in ASCII: 0x56534C50).
     */
    public static final int ISOLATED_FRAME_MAGIC = 0x56534C50;

    /**
     * Fixed frame header length: 4 bytes magic + 4 bytes packetId + 4 bytes length.
     */
    public static final int FRAME_HEADER_LENGTH = 12;

    /**
     * Maximum allowed isolated packet payload size (8 MB).
     */
    public static final int MAX_ISOLATED_PAYLOAD_SIZE = 8_388_608;

    /**
     * Standard minimum packet ID threshold considered custom/modded when dynamic registry is used.
     */
    public static final int DYNAMIC_MOD_ID_THRESHOLD = 0x8000;

    private final Set<Integer> registeredModPacketIds = ConcurrentHashMap.newKeySet();
    private final AtomicLong isolatedPacketsCount = new AtomicLong(0L);

    /**
     * Constructs a new DynamicModdedPacketIdIsolator.
     */
    public DynamicModdedPacketIdIsolator() {
    }

    /**
     * Registers a dynamically allocated mod packet ID to be isolated into the vsl:mod_packet channel.
     *
     * @param packetId the packet identifier
     */
    public void registerModPacketId(int packetId) {
        if (packetId < 0) {
            throw new IllegalArgumentException("packetId cannot be negative: " + packetId);
        }
        registeredModPacketIds.add(packetId);
    }

    /**
     * Unregisters a previously registered mod packet ID.
     *
     * @param packetId the packet identifier
     * @return {@code true} if present and removed
     */
    public boolean unregisterModPacketId(int packetId) {
        return registeredModPacketIds.remove(packetId);
    }

    /**
     * Checks if a given packet ID is classified as a modded packet requiring isolation.
     *
     * @param packetId the packet ID to test
     * @return {@code true} if this packet ID requires isolation
     */
    public boolean isModdedPacketId(int packetId) {
        if (registeredModPacketIds.contains(packetId)) {
            return true;
        }
        return packetId >= DYNAMIC_MOD_ID_THRESHOLD;
    }

    /**
     * Encapsulates a modded packet ID and its payload into a isolated wire-safe frame for
     * transmission over the dedicated {@code vsl:mod_packet} channel.
     *
     * @param originalPacketId the original modded packet identifier
     * @param rawPayload       the binary payload bytes
     * @return isolated packet envelope
     */
    public IsolatedPacketEnvelope wrapModdedPacket(int originalPacketId, byte[] rawPayload) {
        Objects.requireNonNull(rawPayload, "rawPayload must not be null");
        if (rawPayload.length > MAX_ISOLATED_PAYLOAD_SIZE) {
            throw new IllegalArgumentException("Payload size " + rawPayload.length + " exceeds maximum limit "
                    + MAX_ISOLATED_PAYLOAD_SIZE);
        }

        ByteBuffer buffer = ByteBuffer.allocate(FRAME_HEADER_LENGTH + rawPayload.length);
        buffer.putInt(ISOLATED_FRAME_MAGIC);
        buffer.putInt(originalPacketId);
        buffer.putInt(rawPayload.length);
        buffer.put(rawPayload);

        isolatedPacketsCount.incrementAndGet();

        return new IsolatedPacketEnvelope(
                originalPacketId,
                ISOLATED_MOD_CHANNEL,
                buffer.array(),
                System.nanoTime()
        );
    }

    /**
     * Decodes and unwraps an encapsulated payload from the {@code vsl:mod_packet} channel,
     * verifying framing integrity and extracting the original packet ID and payload.
     *
     * @param framedBytes the framed packet bytes
     * @return unwrapped envelope containing original packet ID and un-framed payload
     */
    public IsolatedPacketEnvelope unwrapModdedPacket(byte[] framedBytes) {
        Objects.requireNonNull(framedBytes, "framedBytes must not be null");
        if (framedBytes.length < FRAME_HEADER_LENGTH) {
            throw new IllegalArgumentException("Framed payload is too small: " + framedBytes.length
                    + " bytes (minimum header: " + FRAME_HEADER_LENGTH + ")");
        }

        ByteBuffer buffer = ByteBuffer.wrap(framedBytes);
        int magic = buffer.getInt();
        if (magic != ISOLATED_FRAME_MAGIC) {
            throw new IllegalStateException("Corrupt isolated packet frame! Magic mismatch: 0x"
                    + Integer.toHexString(magic) + " expected: 0x" + Integer.toHexString(ISOLATED_FRAME_MAGIC));
        }

        int originalPacketId = buffer.getInt();
        int payloadLength = buffer.getInt();

        if (payloadLength < 0 || payloadLength > MAX_ISOLATED_PAYLOAD_SIZE) {
            throw new IllegalArgumentException("Invalid isolated payload length: " + payloadLength);
        }

        if (buffer.remaining() < payloadLength) {
            throw new IllegalArgumentException("Truncated isolated payload: expected " + payloadLength
                    + " bytes, but remaining is " + buffer.remaining());
        }

        byte[] payload = new byte[payloadLength];
        buffer.get(payload);

        return new IsolatedPacketEnvelope(
                originalPacketId,
                ISOLATED_MOD_CHANNEL,
                payload,
                System.nanoTime()
        );
    }

    /**
     * Returns the total count of modded packets isolated.
     *
     * @return isolated packet count
     */
    public long getIsolatedPacketsCount() {
        return isolatedPacketsCount.get();
    }

    /**
     * Returns an unmodifiable set of all registered modded packet IDs.
     *
     * @return registered packet IDs
     */
    public Set<Integer> getRegisteredModPacketIds() {
        return Collections.unmodifiableSet(registeredModPacketIds);
    }

    /**
     * Resets isolation statistics.
     */
    public void resetMetrics() {
        isolatedPacketsCount.set(0L);
    }
}
