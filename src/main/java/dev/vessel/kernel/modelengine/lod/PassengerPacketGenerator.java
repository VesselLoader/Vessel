package dev.vessel.kernel.modelengine.lod;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

/**
 * Generates simulated {@code ClientboundSetPassengersPacket} payloads and descriptors
 * linking the vehicle driver mob to all passenger bone entities and hitboxes.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class PassengerPacketGenerator {

    /**
     * Constructs a PassengerPacketGenerator.
     */
    public PassengerPacketGenerator() {
    }

    /**
     * Encodes a synthetic binary passenger packet payload containing the vehicle entity ID
     * followed by the count and IDs of all passenger entities.
     *
     * @param vehicleEntityId vehicle entity ID
     * @param passengerIds    list of passenger entity IDs
     * @return encoded byte array payload
     */
    public byte[] encodePassengerPacket(int vehicleEntityId, List<Integer> passengerIds) {
        Objects.requireNonNull(passengerIds, "passengerIds cannot be null");

        int size = 4 + 4 + (passengerIds.size() * 4);
        ByteBuffer buffer = ByteBuffer.allocate(size);

        buffer.putInt(vehicleEntityId);
        buffer.putInt(passengerIds.size());
        for (int i = 0; i < passengerIds.size(); i++) {
            buffer.putInt(passengerIds.get(i));
        }

        return buffer.array();
    }

    /**
     * Decodes the passenger entity IDs from a synthetic binary passenger packet payload.
     *
     * @param payload binary packet payload
     * @return array of passenger entity IDs
     */
    public int[] decodePassengerIds(byte[] payload) {
        Objects.requireNonNull(payload, "payload cannot be null");
        if (payload.length < 8) {
            throw new IllegalArgumentException("payload length too small: " + payload.length);
        }

        ByteBuffer buffer = ByteBuffer.wrap(payload);
        buffer.getInt(); // Skip vehicle ID
        int count = buffer.getInt();
        if (count < 0 || payload.length < 8 + (count * 4)) {
            throw new IllegalArgumentException("Corrupted passenger count: " + count);
        }

        int[] ids = new int[count];
        for (int i = 0; i < count; i++) {
            ids[i] = buffer.getInt();
        }
        return ids;
    }
}
