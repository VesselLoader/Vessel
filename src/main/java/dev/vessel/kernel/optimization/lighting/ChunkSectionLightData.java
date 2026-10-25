package dev.vessel.kernel.optimization.lighting;

import java.util.Arrays;

/**
 * Compact 4-bit nibble array storing lighting levels (0..15) for a 16x16x16 chunk section
 * in a flat 2048-byte primitive byte array with zero object wrapper overhead.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ChunkSectionLightData {

    public static final int SECTION_SIZE = 16;
    public static final int VOXEL_COUNT = SECTION_SIZE * SECTION_SIZE * SECTION_SIZE; // 4096
    public static final int BYTE_CAPACITY = VOXEL_COUNT / 2; // 2048 bytes

    private final byte[] storage;

    /**
     * Constructs a ChunkSectionLightData initialized to 0.
     */
    public ChunkSectionLightData() {
        this.storage = new byte[BYTE_CAPACITY];
    }

    /**
     * Constructs a ChunkSectionLightData wrapping an existing byte array of 2048 bytes.
     *
     * @param initialData backing byte array
     */
    public ChunkSectionLightData(byte[] initialData) {
        if (initialData.length != BYTE_CAPACITY) {
            throw new IllegalArgumentException("Light data array must be exactly " + BYTE_CAPACITY + " bytes");
        }
        this.storage = Arrays.copyOf(initialData, BYTE_CAPACITY);
    }

    /**
     * Computes the 0..4095 flat index from 0..15 section local coordinates.
     */
    public static int getIndex(int x, int y, int z) {
        return ((y & 15) << 8) | ((z & 15) << 4) | (x & 15);
    }

    /**
     * Gets the 4-bit light level (0..15) at local section coordinate.
     *
     * @param x local X (0..15)
     * @param y local Y (0..15)
     * @param z local Z (0..15)
     * @return light level 0..15
     */
    public int get(int x, int y, int z) {
        int idx = getIndex(x, y, z);
        int byteIdx = idx >> 1;
        byte b = storage[byteIdx];
        if ((idx & 1) == 0) {
            return b & 0x0F;
        } else {
            return (b >> 4) & 0x0F;
        }
    }

    /**
     * Sets the 4-bit light level (0..15) at local section coordinate.
     *
     * @param x local X (0..15)
     * @param y local Y (0..15)
     * @param z local Z (0..15)
     * @param level light level (0..15)
     */
    public void set(int x, int y, int z, int level) {
        int clamped = Math.max(0, Math.min(15, level));
        int idx = getIndex(x, y, z);
        int byteIdx = idx >> 1;
        byte b = storage[byteIdx];
        if ((idx & 1) == 0) {
            storage[byteIdx] = (byte) ((b & 0xF0) | clamped);
        } else {
            storage[byteIdx] = (byte) ((b & 0x0F) | (clamped << 4));
        }
    }

    /**
     * Fills all 4096 voxels in this section with a constant light level.
     *
     * @param level light level (0..15)
     */
    public void fill(int level) {
        int clamped = Math.max(0, Math.min(15, level));
        byte pattern = (byte) (clamped | (clamped << 4));
        Arrays.fill(storage, pattern);
    }

    /**
     * Returns a copy of the raw 2048-byte nibble array.
     *
     * @return copied byte array
     */
    public byte[] toByteArray() {
        return Arrays.copyOf(storage, BYTE_CAPACITY);
    }

    /**
     * Checks if all voxels are completely unlit (all zeroes).
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        for (byte b : storage) {
            if (b != 0) return false;
        }
        return true;
    }
}
