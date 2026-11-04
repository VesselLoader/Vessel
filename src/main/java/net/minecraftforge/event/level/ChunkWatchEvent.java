package net.minecraftforge.event.level;

import net.minecraftforge.eventbus.api.Event;

/**
 * Universal multi-era base class for player chunk watch events.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class ChunkWatchEvent extends Event {

    private final Object player;
    private final Object pos;
    private final Object level;

    /**
     * Constructs a chunk watch event.
     *
     * @param player player observing
     * @param pos    chunk coordinate
     * @param level  server level
     */
    public ChunkWatchEvent(Object player, Object pos, Object level) {
        this.player = player;
        this.pos = pos;
        this.level = level;
    }

    public Object getPlayer() {
        return player;
    }

    public Object getPos() {
        return pos;
    }

    public Object getLevel() {
        return level;
    }
}
