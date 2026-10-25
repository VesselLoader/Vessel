package dev.vessel.kernel.scene;

/**
 * Immutable spatial and structural snapshot of a dynamic multi-block contraption.
 * <p>
 * Supports moving mechanical contraptions (trains, elevators, flying structures, ships)
 * as specified in Architecture Decision 11. Tracks dirty bitmasks for delta compression.
 *
 * @version 1.0.0
 */
public record DynamicContraptionState(
        long contraptionId,
        double posX,
        double posY,
        double posZ,
        float yaw,
        float pitch,
        float roll,
        long structureVersion,
        int dirtyMask
) {
    public static final int DIRTY_POS = 1 << 0;
    public static final int DIRTY_ROT = 1 << 1;
    public static final int DIRTY_STRUCTURE = 1 << 2;

    public boolean hasPositionChanged() {
        return (dirtyMask & DIRTY_POS) != 0;
    }

    public boolean hasRotationChanged() {
        return (dirtyMask & DIRTY_ROT) != 0;
    }

    public boolean hasStructureChanged() {
        return (dirtyMask & DIRTY_STRUCTURE) != 0;
    }

    public boolean isClean() {
        return dirtyMask == 0;
    }
}
