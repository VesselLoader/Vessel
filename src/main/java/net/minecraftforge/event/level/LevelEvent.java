package net.minecraftforge.event.level;

import net.minecraftforge.eventbus.api.Event;

/**
 * Universal multi-era base class for Forge level/world events.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class LevelEvent extends Event {

    private final Object level;

    /**
     * Constructs a level event.
     */
    public LevelEvent() {
        this.level = null;
    }

    /**
     * Constructs a level event with the given level accessor.
     *
     * @param level level accessor
     */
    public LevelEvent(Object level) {
        this.level = level;
    }

    /**
     * Returns the level where this event occurred.
     *
     * @return level accessor or null
     */
    public Object getLevel() {
        return level;
    }
}
