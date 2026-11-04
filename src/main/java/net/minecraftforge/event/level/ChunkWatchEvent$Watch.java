package net.minecraftforge.event.level;

/**
 * Universal multi-era event fired when a player starts watching a chunk.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class ChunkWatchEvent$Watch extends ChunkWatchEvent {

    /**
     * Constructs a chunk watch event.
     *
     * @param player player observing
     * @param pos    chunk coordinate
     * @param level  server level
     */
    public ChunkWatchEvent$Watch(Object player, Object pos, Object level) {
        super(player, pos, level);
    }
}
