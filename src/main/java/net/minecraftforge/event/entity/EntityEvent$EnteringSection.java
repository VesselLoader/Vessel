package net.minecraftforge.event.entity;

/**
 * Universal multi-era event matching bytecode signature EntityEvent$EnteringSection.
 * Adheres strictly to Rule 17 (Single Responsibility Principle - zero inner classes).
 *
 * @version 1.0.0
 */
public class EntityEvent$EnteringSection extends EntityEvent {

    private final long packedOldPos;
    private final long packedNewPos;

    public EntityEvent$EnteringSection() {
        this(null, 0L, 0L);
    }

    public EntityEvent$EnteringSection(Object entity, long packedOldPos, long packedNewPos) {
        super(entity);
        this.packedOldPos = packedOldPos;
        this.packedNewPos = packedNewPos;
    }

    public long getPackedOldPos() {
        return packedOldPos;
    }

    public long getPackedNewPos() {
        return packedNewPos;
    }
}
