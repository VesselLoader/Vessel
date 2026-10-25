package dev.vessel.kernel.network.protocol;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable record representing the result of RakNet MTU frame ingestion and reassembly.
 *
 * @param complete         {@code true} if all split frames have been ingested and reassembly is finished
 * @param coalescedPayload the reassembled full payload bytes, or empty array if incomplete
 * @param compoundId       the compound identifier of the packet
 * @param receivedFrames   the number of frames received so far for this compound
 * @param totalFrames      the expected total number of frames
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record CoalescedMtuResult(
        boolean complete,
        byte[] coalescedPayload,
        int compoundId,
        int receivedFrames,
        int totalFrames
) {

    /**
     * Compact constructor validating non-null requirements.
     */
    public CoalescedMtuResult {
        Objects.requireNonNull(coalescedPayload, "coalescedPayload must not be null");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CoalescedMtuResult other)) {
            return false;
        }
        return complete == other.complete
                && compoundId == other.compoundId
                && receivedFrames == other.receivedFrames
                && totalFrames == other.totalFrames
                && Arrays.equals(coalescedPayload, other.coalescedPayload);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(complete, compoundId, receivedFrames, totalFrames);
        result = 31 * result + Arrays.hashCode(coalescedPayload);
        return result;
    }

    @Override
    public String toString() {
        return "CoalescedMtuResult{"
                + "complete=" + complete
                + ", compoundId=" + compoundId
                + ", receivedFrames=" + receivedFrames
                + ", totalFrames=" + totalFrames
                + ", payloadLength=" + coalescedPayload.length
                + '}';
    }
}
