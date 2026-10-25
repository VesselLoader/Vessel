package dev.vessel.kernel.network.protocol;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable record representing an isolated modded packet envelope routed through the
 * isolated mod packet namespace.
 *
 * @param originalPacketId the original modded packet identifier
 * @param targetChannel    the namespaced channel identifier (e.g. "vsl:mod_packet")
 * @param payload          the payload bytes of the encapsulated packet
 * @param timestampNanos   timestamp in nanoseconds when this envelope was constructed
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record IsolatedPacketEnvelope(
        int originalPacketId,
        String targetChannel,
        byte[] payload,
        long timestampNanos
) {

    /**
     * Compact constructor validating non-null requirements and range checks.
     */
    public IsolatedPacketEnvelope {
        Objects.requireNonNull(targetChannel, "targetChannel must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        if (originalPacketId < 0) {
            throw new IllegalArgumentException("originalPacketId cannot be negative: " + originalPacketId);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof IsolatedPacketEnvelope other)) {
            return false;
        }
        return originalPacketId == other.originalPacketId
                && timestampNanos == other.timestampNanos
                && Objects.equals(targetChannel, other.targetChannel)
                && Arrays.equals(payload, other.payload);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(originalPacketId, targetChannel, timestampNanos);
        result = 31 * result + Arrays.hashCode(payload);
        return result;
    }

    @Override
    public String toString() {
        return "IsolatedPacketEnvelope{"
                + "originalPacketId=" + originalPacketId
                + ", targetChannel='" + targetChannel + '\''
                + ", payloadLength=" + payload.length
                + ", timestampNanos=" + timestampNanos
                + '}';
    }
}
