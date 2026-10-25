package dev.vessel.kernel.optimization.block;

import java.util.Arrays;

/**
 * FerriteCore-inspired primitive directional neighbor state transition table.
 * <p>
 * In vanilla Minecraft, every block state stores directional neighbor relationships
 * (e.g., fence connections, stairs shape updates, redstone wire alignment) using
 * object maps or multidimensional pointer arrays across 6 cardinal directions:
 * DOWN (0), UP (1), NORTH (2), SOUTH (3), WEST (4), EAST (5).
 * This table stores all neighbor transitions in a single flat contiguous primitive {@code int[]} array,
 * indexed by {@code (sourceStateId * 6) + direction}, eliminating pointer chasing and GC overhead.
 *
 * @version 1.0.0
 */
public final class CompactBlockNeighborTable {

    public static final int DIR_DOWN = 0;
    public static final int DIR_UP = 1;
    public static final int DIR_NORTH = 2;
    public static final int DIR_SOUTH = 3;
    public static final int DIR_WEST = 4;
    public static final int DIR_EAST = 5;
    public static final int DIRECTIONS_COUNT = 6;

    private int[] table;
    private int maxStateId;

    /**
     * Constructs a neighbor transition table with an initial capacity of block states.
     *
     * @param initialCapacity number of initial states supported
     */
    public CompactBlockNeighborTable(int initialCapacity) {
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("initialCapacity must be positive: " + initialCapacity);
        }
        this.table = new int[initialCapacity * DIRECTIONS_COUNT];
        Arrays.fill(this.table, -1); // -1 indicates default/no transition
        this.maxStateId = initialCapacity - 1;
    }

    /**
     * Registers a neighbor state transition.
     *
     * @param sourceStateId the ID of the block state being updated
     * @param direction     direction index (0 to 5)
     * @param targetStateId the resulting block state ID after the neighbor change
     */
    public synchronized void setNeighborState(int sourceStateId, int direction, int targetStateId) {
        validateDirection(direction);
        if (sourceStateId < 0) {
            throw new IndexOutOfBoundsException("sourceStateId cannot be negative: " + sourceStateId);
        }

        ensureCapacity(sourceStateId);
        int index = sourceStateId * DIRECTIONS_COUNT + direction;
        table[index] = targetStateId;
    }

    /**
     * Retrieves the resulting state ID when a neighbor updates in the given direction.
     *
     * @param sourceStateId source block state ID
     * @param direction     direction index (0 to 5)
     * @return resulting state ID, or -1 if no explicit transition is defined
     */
    public int getNeighborState(int sourceStateId, int direction) {
        validateDirection(direction);
        if (sourceStateId < 0 || sourceStateId > maxStateId) {
            return -1;
        }
        int index = sourceStateId * DIRECTIONS_COUNT + direction;
        if (index >= table.length) {
            return -1;
        }
        return table[index];
    }

    private void ensureCapacity(int requiredStateId) {
        if (requiredStateId <= maxStateId) {
            return;
        }
        int newCapacity = Math.max(requiredStateId + 1, (maxStateId + 1) * 2);
        int[] newTable = new int[newCapacity * DIRECTIONS_COUNT];
        Arrays.fill(newTable, -1);
        System.arraycopy(table, 0, newTable, 0, table.length);
        this.table = newTable;
        this.maxStateId = newCapacity - 1;
    }

    private static void validateDirection(int direction) {
        if (direction < 0 || direction >= DIRECTIONS_COUNT) {
            throw new IndexOutOfBoundsException("Invalid direction: " + direction + ". Expected 0 to 5.");
        }
    }

    /**
     * Returns the current capacity in number of states.
     *
     * @return state capacity
     */
    public int getCapacity() {
        return maxStateId + 1;
    }

    /**
     * Returns the memory consumption in bytes of the primitive backing array.
     *
     * @return memory size in bytes
     */
    public long getMemorySizeBytes() {
        return (long) table.length * Integer.BYTES;
    }
}
