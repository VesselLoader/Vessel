package dev.vessel.kernel.testmatrix;

/**
 * High-performance bit-packing codec decomposing 3D block and chunk coordinates into primitive 64-bit longs.
 *
 * <p>Enables HotSpot C2 escape analysis to perform aggressive scalar replacement on coordinate vectors.</p>
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class PackedCoordinateCodec {

    private static final int X_BITS = 26;
    private static final int Z_BITS = 26;
    private static final int Y_BITS = 12;

    private static final long X_MASK = (1L << X_BITS) - 1L;
    private static final long Y_MASK = (1L << Y_BITS) - 1L;
    private static final long Z_MASK = (1L << Z_BITS) - 1L;

    private static final int Y_SHIFT = 0;
    private static final int Z_SHIFT = Y_BITS;
    private static final int X_SHIFT = Y_BITS + Z_BITS;

    private PackedCoordinateCodec() {
    }

    /**
     * Packs 3D block coordinates into a single primitive long without object allocation.
     *
     * @param x block x coordinate
     * @param y block y coordinate (-2048 to 2047)
     * @param z block z coordinate
     * @return 64-bit packed coordinate
     */
    public static long packBlockPos(int x, int y, int z) {
        return (((long) x & X_MASK) << X_SHIFT)
                | (((long) z & Z_MASK) << Z_SHIFT)
                | (((long) y & Y_MASK) << Y_SHIFT);
    }

    /**
     * Unpacks the X coordinate from a packed block long.
     *
     * @param packed packed coordinate long
     * @return block X coordinate
     */
    public static int unpackX(long packed) {
        return (int) (packed << (64 - X_SHIFT - X_BITS) >> (64 - X_BITS));
    }

    /**
     * Unpacks the Y coordinate from a packed block long.
     *
     * @param packed packed coordinate long
     * @return block Y coordinate
     */
    public static int unpackY(long packed) {
        return (int) (packed << (64 - Y_BITS) >> (64 - Y_BITS));
    }

    /**
     * Unpacks the Z coordinate from a packed block long.
     *
     * @param packed packed coordinate long
     * @return block Z coordinate
     */
    public static int unpackZ(long packed) {
        return (int) (packed << (64 - Z_SHIFT - Z_BITS) >> (64 - Z_BITS));
    }

    /**
     * Packs 2D chunk coordinates (chunkX, chunkZ) into a single primitive long.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return 64-bit packed chunk key
     */
    public static long packChunkPos(int chunkX, int chunkZ) {
        return (((long) chunkX) & 0xFFFFFFFFL) | ((((long) chunkZ) & 0xFFFFFFFFL) << 32);
    }

    /**
     * Unpacks chunk X from packed chunk long.
     *
     * @param packed packed chunk key
     * @return chunk X
     */
    public static int unpackChunkX(long packed) {
        return (int) (packed & 0xFFFFFFFFL);
    }

    /**
     * Unpacks chunk Z from packed chunk long.
     *
     * @param packed packed chunk key
     * @return chunk Z
     */
    public static int unpackChunkZ(long packed) {
        return (int) (packed >>> 32);
    }
}
