package net.minecraftforge.event;

/**
 * Universal cross-era shim for Forge TickEvent$LevelTickEvent.
 * Fired at START and END of world level tick.
 * Maintained under VESSEL Rule 17: Standalone single-purpose class.
 *
 * @version 1.0.0
 */
public class TickEvent$LevelTickEvent extends TickEvent {

    private final Object level;

    public TickEvent$LevelTickEvent() {
        this(TickPhase.START, null);
    }

    public TickEvent$LevelTickEvent(TickPhase phase, Object level) {
        super("LEVEL", phase);
        this.level = level;
    }

    public Object getLevel() {
        return this.level;
    }
}
