package dev.vessel.kernel.scene;

import java.util.Arrays;
import java.util.Objects;

/**
 * Consolidated 128-tick spatial delta batch packet for dynamic multi-block contraptions (Create / Valkyrien).
 * <p>
 * Implements Architecture Decision 11, coalescing high-frequency sub-tick translation, rotation, and dirty flags
 * across a 128-tick history window into a single atomic network synchronization payload.
 * </p>
 *
 * @param contraptionId     unique identifier of the contraption entity
 * @param startTick         starting tick of the delta window
 * @param endTick           ending tick of the delta window
 * @param frameCount        number of valid movement frames included in this batch
 * @param deltaX            cumulative displacement along X axis
 * @param deltaY            cumulative displacement along Y axis
 * @param deltaZ            cumulative displacement along Z axis
 * @param deltaYaw          cumulative angular rotation change in yaw
 * @param deltaPitch        cumulative angular rotation change in pitch
 * @param deltaRoll         cumulative angular rotation change in roll
 * @param combinedDirtyMask bitwise OR aggregation of all dirty mutation flags
 * @param serializedPayload packed binary payload ready for zero-copy socket transmission
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record CreateContraptionBatchPacket(
        long contraptionId,
        long startTick,
        long endTick,
        int frameCount,
        double deltaX,
        double deltaY,
        double deltaZ,
        float deltaYaw,
        float deltaPitch,
        float deltaRoll,
        int combinedDirtyMask,
        byte[] serializedPayload
) {
    /**
     * Validates packet boundaries and invariants.
     */
    public CreateContraptionBatchPacket {
        if (startTick < 0) {
            throw new IllegalArgumentException("startTick cannot be negative: " + startTick);
        }
        if (endTick < startTick) {
            throw new IllegalArgumentException("endTick (" + endTick + ") cannot precede startTick (" + startTick + ")");
        }
        if (frameCount < 0) {
            throw new IllegalArgumentException("frameCount cannot be negative: " + frameCount);
        }
        Objects.requireNonNull(serializedPayload, "serializedPayload cannot be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CreateContraptionBatchPacket that)) return false;
        return contraptionId == that.contraptionId &&
                startTick == that.startTick &&
                endTick == that.endTick &&
                frameCount == that.frameCount &&
                Double.compare(that.deltaX, deltaX) == 0 &&
                Double.compare(that.deltaY, deltaY) == 0 &&
                Double.compare(that.deltaZ, deltaZ) == 0 &&
                Float.compare(that.deltaYaw, deltaYaw) == 0 &&
                Float.compare(that.deltaPitch, deltaPitch) == 0 &&
                Float.compare(that.deltaRoll, deltaRoll) == 0 &&
                combinedDirtyMask == that.combinedDirtyMask &&
                Arrays.equals(serializedPayload, that.serializedPayload);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(contraptionId, startTick, endTick, frameCount, deltaX, deltaY, deltaZ, deltaYaw, deltaPitch, deltaRoll, combinedDirtyMask);
        result = 31 * result + Arrays.hashCode(serializedPayload);
        return result;
    }
}
