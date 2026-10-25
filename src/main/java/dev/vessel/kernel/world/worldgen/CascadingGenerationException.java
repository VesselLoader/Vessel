package dev.vessel.kernel.world.worldgen;

/**
 * Exception thrown when cascading world generation exceeds maximum recursion depth
 * or when a cross-thread circular generation dependency is detected.
 * <p>
 * Directly resolves ERR-33 and WGN-0001..0250.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class CascadingGenerationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final long chunkKey;
    private final int depth;

    /**
     * Constructs a new CascadingGenerationException.
     *
     * @param chunkKey packed 64-bit coordinate of the offending chunk
     * @param depth recursion depth at which generation was halted
     * @param reason explanatory message
     */
    public CascadingGenerationException(long chunkKey, int depth, String reason) {
        super(String.format("Cascading world generation halted for chunk 0x%016X at depth %d: %s",
                chunkKey, depth, reason));
        this.chunkKey = chunkKey;
        this.depth = depth;
    }

    /**
     * Returns the packed chunk key that triggered this exception.
     *
     * @return 64-bit packed chunk coordinate
     */
    public long chunkKey() {
        return chunkKey;
    }

    /**
     * Returns the recursion depth when halted.
     *
     * @return cascading depth
     */
    public int depth() {
        return depth;
    }
}
