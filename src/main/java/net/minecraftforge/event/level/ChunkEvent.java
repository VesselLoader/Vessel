package net.minecraftforge.event.level;

/**
 * Universal multi-era base class for Forge chunk events.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class ChunkEvent extends LevelEvent {

    private final Object chunk;

    /**
     * Constructs a chunk event with no associated chunk.
     */
    public ChunkEvent() {
        super();
        this.chunk = null;
    }

    /**
     * Constructs a chunk event with the specified chunk access instance.
     *
     * @param chunk chunk access instance
     */
    public ChunkEvent(Object chunk) {
        super();
        this.chunk = chunk;
    }

    /**
     * Constructs a chunk event with the specified chunk access and level.
     *
     * @param chunk chunk access instance
     * @param level level instance
     */
    public ChunkEvent(Object chunk, Object level) {
        super(level);
        this.chunk = chunk;
    }

    /**
     * Returns the chunk target of this event.
     *
     * @return chunk instance or null
     */
    public Object getChunk() {
        return chunk;
    }
}
