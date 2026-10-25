package dev.vessel.kernel.world.worldgen;

/**
 * Thread-local stack tracker maintaining active chunk generation recursion depth.
 * <p>
 * Eliminates heap allocations during recursive worldgen traversal by using
 * a fixed-size primitive long array for the chunk call stack.
 * </p>
 * <p>
 * Directly resolves ERR-33 and WGN-0001..0250.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class CascadingThreadDepthTracker {

    /**
     * Maximum supported recursion depth for the fixed-size primitive stack.
     */
    public static final int MAX_TRACKED_DEPTH = 32;

    private static final ThreadLocal<CascadingThreadDepthTracker> THREAD_LOCAL_TRACKER =
            ThreadLocal.withInitial(CascadingThreadDepthTrackerSupplier.INSTANCE);

    private final long[] chunkStack;
    private int currentDepth;

    /**
     * Constructs a thread-local depth tracker.
     */
    public CascadingThreadDepthTracker() {
        this.chunkStack = new long[MAX_TRACKED_DEPTH];
        this.currentDepth = 0;
    }

    /**
     * Obtains the tracker instance bound to the caller thread.
     *
     * @return thread-local tracker instance
     */
    public static CascadingThreadDepthTracker current() {
        return THREAD_LOCAL_TRACKER.get();
    }

    /**
     * Returns the current generation recursion depth for this thread.
     *
     * @return current depth (0 indicates top-level generation)
     */
    public int currentDepth() {
        return currentDepth;
    }

    /**
     * Pushes a chunk coordinate onto the active generation stack.
     *
     * @param chunkKey packed 64-bit chunk coordinate
     * @throws IllegalStateException if the stack exceeds MAX_TRACKED_DEPTH
     */
    public void push(long chunkKey) {
        if (currentDepth >= MAX_TRACKED_DEPTH) {
            throw new IllegalStateException("Chunk generation stack exceeded maximum depth: " + MAX_TRACKED_DEPTH);
        }
        chunkStack[currentDepth++] = chunkKey;
    }

    /**
     * Pops the top chunk coordinate from the active generation stack.
     *
     * @return packed 64-bit chunk coordinate
     * @throws IllegalStateException if the stack is empty
     */
    public long pop() {
        if (currentDepth <= 0) {
            throw new IllegalStateException("Attempted to pop an empty chunk generation stack");
        }
        return chunkStack[--currentDepth];
    }

    /**
     * Checks if the specified chunk coordinate is currently being generated higher up on this thread's stack.
     *
     * @param chunkKey packed 64-bit chunk coordinate
     * @return {@code true} if present in call stack (circular self-recursion)
     */
    public boolean contains(long chunkKey) {
        for (int i = 0; i < currentDepth; i++) {
            if (chunkStack[i] == chunkKey) {
                return true;
            }
        }
        return false;
    }

    /**
     * Peeks at the top chunk coordinate currently being generated.
     *
     * @return packed chunk key, or {@code 0L} if empty
     */
    public long peek() {
        if (currentDepth <= 0) {
            return 0L;
        }
        return chunkStack[currentDepth - 1];
    }

    /**
     * Resets the tracker stack to an empty state.
     */
    public void clear() {
        currentDepth = 0;
    }
}
