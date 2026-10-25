package dev.vessel.kernel.network.protocol;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable record representing a single fragmented or coalesced RakNet MTU frame.
 *
 * @param compoundId compound identifier grouping all fragments of a single logical packet
 * @param splitIndex zero-based fragment index within the split compound
 * @param splitCount total number of fragments composing the split compound
 * @param payload    fragment slice payload bytes
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record RakNetMtuFrame(
        int compoundId,
        int splitIndex,
        int splitCount,
        byte[] payload
) {

    /**
     * Compact constructor validating non-null requirements and range boundaries.
     */
    public RakNetMtuFrame {
        if (compoundId < 0) {
            throw new IllegalArgumentException("compoundId cannot be negative: " + compoundId);
        }
        if (splitCount <= 0) {
            throw new IllegalArgumentException("splitCount must be greater than 0: " + splitCount);
        }
        if (splitIndex < 0 || splitIndex >= splitCount) {
            throw new IllegalArgumentException("splitIndex out of bounds [0, " + splitCount + "): " + splitIndex);
        }
        Objects.requireNonNull(payload, "payload must not be null");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RakNetMtuFrame other)) {
            return false;
        }
        return compoundId == other.compoundId
                && splitIndex == other.splitIndex
                && splitCount == other.splitCount
                && Arrays.equals(payload, other.payload);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(compoundId, splitIndex, splitCount);
        result = 31 * result + Arrays.hashCode(payload);
        return result;
    }

    @Override
    public String toString() {
        return "RakNetMtuFrame{"
                + "compoundId=" + compoundId
                + ", splitIndex=" + splitIndex
                + ", splitCount=" + splitCount
                + ", payloadLength=" + payload.length
                + '}';
    }
}
