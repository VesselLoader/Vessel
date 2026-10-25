package dev.vessel.kernel.adapter.forge;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable carrier for a Forge custom payload channel message.
 *
 * @param channelName  the namespaced channel identifier (e.g., "create:contraption")
 * @param payload      binary content bytes
 * @param direction    network communication direction
 * @param connectionId identifier of the client connection or session
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record ForgeChannelMessage(
        String channelName,
        byte[] payload,
        ForgeNetworkDirection direction,
        String connectionId
) {

    /**
     * Compact constructor validating non-null arguments and defensively copying payload.
     */
    public ForgeChannelMessage {
        Objects.requireNonNull(channelName, "channelName must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(direction, "direction must not be null");
        Objects.requireNonNull(connectionId, "connectionId must not be null");
        payload = Arrays.copyOf(payload, payload.length);
    }

    /**
     * Obtains a defensive clone of the binary payload.
     *
     * @return cloned byte array
     */
    @Override
    public byte[] payload() {
        return Arrays.copyOf(payload, payload.length);
    }

    /**
     * Returns the size of the payload in bytes without allocation.
     *
     * @return byte count
     */
    public int payloadLength() {
        return payload.length;
    }
}
