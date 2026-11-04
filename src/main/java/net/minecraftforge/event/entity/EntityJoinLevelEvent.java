package net.minecraftforge.event.entity;

/**
 * Universal cross-era shim for Forge EntityJoinLevelEvent.
 * Fired when an entity enters a level/world.
 * Maintained under VESSEL Rule 17: Standalone single-purpose class.
 *
 * @version 1.0.0
 */
public class EntityJoinLevelEvent extends EntityEvent {

    private final Object level;
    private final boolean loadedFromDisk;

    public EntityJoinLevelEvent() {
        this(null, null, false);
    }

    public EntityJoinLevelEvent(Object entity, Object level) {
        this(entity, level, false);
    }

    public EntityJoinLevelEvent(Object entity, Object level, boolean loadedFromDisk) {
        super(entity);
        this.level = level;
        this.loadedFromDisk = loadedFromDisk;
    }

    public Object getLevel() {
        return this.level;
    }

    public boolean loadedFromDisk() {
        return this.loadedFromDisk;
    }
}
