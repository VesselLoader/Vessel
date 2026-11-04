package net.minecraftforge.event.level;

/**
 * Universal multi-era event fired when a chunk is loaded into a level.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class ChunkEvent$Load extends ChunkEvent {

    private final boolean newChunk;

    /**
     * Constructs a chunk load event.
     *
     * @param chunk chunk instance
     * @param newChunk whether this chunk was newly generated
     */
    public ChunkEvent$Load(Object chunk, boolean newChunk) {
        super(chunk);
        this.newChunk = newChunk;
    }

    /**
     * Constructs a chunk load event with level.
     *
     * @param chunk chunk instance
     * @param level level instance
     * @param newChunk whether this chunk was newly generated
     */
    public ChunkEvent$Load(Object chunk, Object level, boolean newChunk) {
        super(chunk, level);
        this.newChunk = newChunk;
    }

    /**
     * Returns whether this chunk was newly generated.
     *
     * @return {@code true} if newly generated
     */
    public boolean isNewChunk() {
        return newChunk;
    }
}
