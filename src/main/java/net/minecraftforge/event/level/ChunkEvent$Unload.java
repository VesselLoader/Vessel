package net.minecraftforge.event.level;

/**
 * Universal multi-era event fired when a chunk is unloaded from a level.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class ChunkEvent$Unload extends ChunkEvent {

    /**
     * Constructs a chunk unload event.
     *
     * @param chunk chunk instance
     */
    public ChunkEvent$Unload(Object chunk) {
        super(chunk);
    }

    /**
     * Constructs a chunk unload event with level.
     *
     * @param chunk chunk instance
     * @param level level instance
     */
    public ChunkEvent$Unload(Object chunk, Object level) {
        super(chunk, level);
    }
}
