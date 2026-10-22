package dev.vessel.kernel.optimization.chunk;

import java.util.Arrays;

/**
 * Lithium-inspired high-speed chunk section palette and block storage.
 * <p>
 * A Minecraft chunk section contains 16x16x16 = 4096 block positions.
 * In vanilla Minecraft, querying a block position requires resolving nested palette structures
 * and bit arrays. In homogeneous sections (e.g., pure air in the sky, or pure stone deep underground),
 * allocating 4096 entries wastes tens of megabytes of RAM.
 * This class provides adaptive single-value compact storage for homogeneous sections,
 * and flat 12-bit coordinate packing {@code (y << 8) | (z << 4) | x} for multi-state sections,
 * enabling sub-nanosecond block state lookups with zero heap allocations.
 *
 * @version 1.0.1
 */
public final class FastChunkSectionPalette {

    public static final int SECTION_SIZE = 16;
    public static final int VOLUME = SECTION_SIZE * SECTION_SIZE * SECTION_SIZE; // 4096

    private volatile boolean homogeneous;
    private volatile int singleStateId;
    private volatile short[] stateData; // null when homogeneous
    private volatile int nonAirCount;

    /**
     * Constructs a homogeneous chunk section initialized to the given default state (e.g., 0 for air).
     *
     * @param defaultStateId the initial state ID for all 4096 positions
     */
    public FastChunkSectionPalette(int defaultStateId) {
        this.homogeneous = true;
        this.singleStateId = defaultStateId;
        this.stateData = null;
        this.nonAirCount = (defaultStateId == 0) ? 0 : VOLUME;
    }

    /**
     * Computes the flat 12-bit index for coordinates within a 16x16x16 section.
     *
     * @param x local X (0-15)
     * @param y local Y (0-15)
     * @param z local Z (0-15)
     * @return flat index (0-4095)
     */
    public static int getIndex(int x, int y, int z) {
        if ((x & ~0xF) != 0 || (y & ~0xF) != 0 || (z & ~0xF) != 0) {
            throw new IndexOutOfBoundsException("Local section coordinates must be 0-15. Got: (" + x + ", " + y + ", " + z + ")");
        }
        return (y << 8) | (z << 4) | x;
    }

    /**
     * Retrieves the block state ID at local coordinates (x, y, z).
     *
     * @param x local X (0-15)
     * @param y local Y (0-15)
     * @param z local Z (0-15)
     * @return block state ID
     */
    public int getBlockStateId(int x, int y, int z) {
        int index = getIndex(x, y, z);
        if (homogeneous) {
            return singleStateId;
        }
        short[] data = this.stateData;
        if (data != null) {
            return data[index] & 0xFFFF;
        }
        return singleStateId;
    }

    /**
     * Sets the block state ID at local coordinates (x, y, z).
     *
     * @param x       local X (0-15)
     * @param y       local Y (0-15)
     * @param z       local Z (0-15)
     * @param stateId new block state ID
     * @return the previous block state ID at that position
     */
    public synchronized int setBlockStateId(int x, int y, int z, int stateId) {
        int index = getIndex(x, y, z);

        if (homogeneous) {
            if (singleStateId == stateId) {
                return stateId; // no change needed
            }
            // Expand from homogeneous to full array
            short[] newData = new short[VOLUME];
            Arrays.fill(newData, (short) (singleStateId & 0xFFFF));
            int oldState = singleStateId;
            newData[index] = (short) (stateId & 0xFFFF);
            this.stateData = newData;
            this.homogeneous = false;

            recomputeNonAirCount();
            return oldState;
        }

        int oldState = stateData[index] & 0xFFFF;
        if (oldState == stateId) {
            return oldState;
        }

        stateData[index] = (short) (stateId & 0xFFFF);

        if (oldState == 0 && stateId != 0) {
            nonAirCount++;
        } else if (oldState != 0 && stateId == 0) {
            nonAirCount--;
        }

        return oldState;
    }

    /**
     * Recomputes non-air block count and compacts to homogeneous if all blocks share the same ID.
     */
    public synchronized void compact() {
        if (homogeneous || stateData == null) {
            return;
        }

        int first = stateData[0] & 0xFFFF;
        boolean allSame = true;
        int nonAir = 0;

        for (int i = 0; i < VOLUME; i++) {
            int val = stateData[i] & 0xFFFF;
            if (val != 0) {
                nonAir++;
            }
            if (val != first) {
                allSame = false;
            }
        }

        this.nonAirCount = nonAir;

        if (allSame) {
            this.singleStateId = first;
            this.stateData = null;
            this.homogeneous = true;
        }
    }

    private void recomputeNonAirCount() {
        if (stateData == null) {
            this.nonAirCount = (singleStateId == 0) ? 0 : VOLUME;
            return;
        }
        int count = 0;
        for (short s : stateData) {
            if ((s & 0xFFFF) != 0) {
                count++;
            }
        }
        this.nonAirCount = count;
    }

    /**
     * Checks if this section is currently stored as a single homogeneous value.
     *
     * @return true if homogeneous
     */
    public boolean isHomogeneous() {
        return homogeneous;
    }

    /**
     * Returns the single state ID if homogeneous.
     *
     * @return state ID
     */
    public int getSingleStateId() {
        return singleStateId;
    }

    /**
     * Returns the number of non-air blocks in this section.
     *
     * @return non-air count
     */
    public int getNonAirBlockCount() {
        return nonAirCount;
    }
}
