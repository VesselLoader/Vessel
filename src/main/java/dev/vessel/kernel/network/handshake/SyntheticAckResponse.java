package dev.vessel.kernel.network.handshake;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable record representing a synthesized client acknowledgment response packet.
 *
 * @param channel       the target packet channel identifier
 * @param payload       the binary payload bytes of the synthetic ACK packet
 * @param phase         the handshake phase represented by this ACK
 * @param durationNanos execution time in nanoseconds taken to synthesize this packet
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record SyntheticAckResponse(
        String channel,
        byte[] payload,
        SyntheticHandshakePhase phase,
        long durationNanos
) {

    /**
     * Compact constructor validating non-null requirements.
     */
    public SyntheticAckResponse {
        Objects.requireNonNull(channel, "channel must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(phase, "phase must not be null");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SyntheticAckResponse other)) {
            return false;
        }
        return durationNanos == other.durationNanos
                && Objects.equals(channel, other.channel)
                && Objects.equals(phase, other.phase)
                && Arrays.equals(payload, other.payload);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(channel, phase, durationNanos);
        result = 31 * result + Arrays.hashCode(payload);
        return result;
    }

    @Override
    public String toString() {
        return "SyntheticAckResponse{"
                + "channel='" + channel + '\''
                + ", payloadLength=" + payload.length
                + ", phase=" + phase
                + ", durationNanos=" + durationNanos
                + '}';
    }
}
