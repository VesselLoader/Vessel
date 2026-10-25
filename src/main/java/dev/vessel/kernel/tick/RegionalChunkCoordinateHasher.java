package dev.vessel.kernel.tick;

/**
 * Zero-allocation spatial coordinate hasher for Folia Regional Actor Strands.
 * <p>
 * Projects chunk coordinates {@code (chunkX, chunkZ)} to their corresponding
 * 16&times;16 chunk region strand {@code (regionX, regionZ)} with O(1) complexity:
 * <pre>
 * regionX = chunkX &gt;&gt; 4
 * regionZ = chunkZ &gt;&gt; 4
 * </pre>
 * All coordinate packing and hashing operations operate exclusively on 64-bit primitive longs
 * to eliminate heap allocations on hot server tick paths.
 * </p>
 * <p>
 * Directly resolves ERR-30 and FOL-021..040.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class RegionalChunkCoordinateHasher {

    /**
     * Bit-shift required to convert chunk coordinates to region strand coordinates.
     */
    public static final int REGION_SHIFT = 4;

    /**
     * Size of one region strand in chunk units (16 chunks &times; 16 chunks).
     */
    public static final int REGION_SIZE = 1 << REGION_SHIFT;

    /**
     * Bitmask to extract chunk offset within its region (0..15).
     */
    public static final int CHUNK_IN_REGION_MASK = REGION_SIZE - 1;

    /**
     * Bit-shift required to convert block coordinates to region strand coordinates.
     */
    public static final int BLOCK_REGION_SHIFT = 8;

    /**
     * Size of one region strand in block units (256 blocks &times; 256 blocks).
     */
    public static final int BLOCK_REGION_SIZE = 1 << BLOCK_REGION_SHIFT;

    /**
     * Bitmask to extract block offset within its region (0..255).
     */
    public static final int BLOCK_IN_REGION_MASK = BLOCK_REGION_SIZE - 1;

    /**
     * Bitmask for packing 32-bit signed integers into low 32 bits of a 64-bit long.
     */
    private static final long INT_MASK = 0xFFFFFFFFL;

    /**
     * Golden ratio 64-bit multiplier for Murmur3-style avalanching hash distribution.
     */
    private static final long PHI_MIXER = 0x9E3779B97F4A7C15L;

    private RegionalChunkCoordinateHasher() {
        // Pure utility class
    }

    /**
     * Maps chunk coordinates to an immutable packed 64-bit region key.
     *
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return 64-bit packed region key (high 32 bits = regionX, low 32 bits = regionZ)
     */
    public static long toRegionKey(int chunkX, int chunkZ) {
        int regX = chunkX >> REGION_SHIFT;
        int regZ = chunkZ >> REGION_SHIFT;
        return (((long) regX) << 32) | (regZ & INT_MASK);
    }

    /**
     * Maps block coordinates directly to a packed 64-bit region key.
     *
     * @param blockX block X coordinate
     * @param blockZ block Z coordinate
     * @return 64-bit packed region key
     */
    public static long blockToRegionKey(int blockX, int blockZ) {
        int regX = blockX >> BLOCK_REGION_SHIFT;
        int regZ = blockZ >> BLOCK_REGION_SHIFT;
        return (((long) regX) << 32) | (regZ & INT_MASK);
    }

    /**
     * Maps chunk coordinate X to region coordinate X.
     *
     * @param chunkX chunk X coordinate
     * @return region X coordinate
     */
    public static int toRegionX(int chunkX) {
        return chunkX >> REGION_SHIFT;
    }

    /**
     * Maps chunk coordinate Z to region coordinate Z.
     *
     * @param chunkZ chunk Z coordinate
     * @return region Z coordinate
     */
    public static int toRegionZ(int chunkZ) {
        return chunkZ >> REGION_SHIFT;
    }

    /**
     * Extracts the region X coordinate from a packed 64-bit region key.
     *
     * @param regionKey packed 64-bit region key
     * @return signed 32-bit region X
     */
    public static int unpackRegionX(long regionKey) {
        return (int) (regionKey >> 32);
    }

    /**
     * Extracts the region Z coordinate from a packed 64-bit region key.
     *
     * @param regionKey packed 64-bit region key
     * @return signed 32-bit region Z
     */
    public static int unpackRegionZ(long regionKey) {
        return (int) regionKey;
    }

    /**
     * Packs two chunk coordinates into a single 64-bit chunk key.
     *
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return 64-bit packed chunk key
     */
    public static long toPackedChunkKey(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) | (chunkZ & INT_MASK);
    }

    /**
     * Extracts the chunk X coordinate from a packed chunk key.
     *
     * @param chunkKey packed 64-bit chunk key
     * @return chunk X coordinate
     */
    public static int unpackChunkX(long chunkKey) {
        return (int) (chunkKey >> 32);
    }

    /**
     * Extracts the chunk Z coordinate from a packed chunk key.
     *
     * @param chunkKey packed 64-bit chunk key
     * @return chunk Z coordinate
     */
    public static int unpackChunkZ(long chunkKey) {
        return (int) chunkKey;
    }

    /**
     * Extracts the chunk offset relative to the region origin along the X-axis (0..15).
     *
     * @param chunkX chunk X coordinate
     * @return relative chunk X in region
     */
    public static int toChunkRelativeX(int chunkX) {
        return chunkX & CHUNK_IN_REGION_MASK;
    }

    /**
     * Extracts the chunk offset relative to the region origin along the Z-axis (0..15).
     *
     * @param chunkZ chunk Z coordinate
     * @return relative chunk Z in region
     */
    public static int toChunkRelativeZ(int chunkZ) {
        return chunkZ & CHUNK_IN_REGION_MASK;
    }

    /**
     * Evaluates if two chunk coordinates belong to the same Folia region strand.
     *
     * @param chunkX1 first chunk X
     * @param chunkZ1 first chunk Z
     * @param chunkX2 second chunk X
     * @param chunkZ2 second chunk Z
     * @return {@code true} if both chunks belong to the exact same region
     */
    public static boolean isSameRegion(int chunkX1, int chunkZ1, int chunkX2, int chunkZ2) {
        return ((chunkX1 >> REGION_SHIFT) == (chunkX2 >> REGION_SHIFT))
                && ((chunkZ1 >> REGION_SHIFT) == (chunkZ2 >> REGION_SHIFT));
    }

    /**
     * Evaluates if two packed chunk keys belong to the same Folia region strand.
     *
     * @param chunkKey1 packed key of first chunk
     * @param chunkKey2 packed key of second chunk
     * @return {@code true} if both chunks belong to the exact same region
     */
    public static boolean isSameRegionByPackedKey(long chunkKey1, long chunkKey2) {
        int x1 = (int) (chunkKey1 >> 32);
        int z1 = (int) chunkKey1;
        int x2 = (int) (chunkKey2 >> 32);
        int z2 = (int) chunkKey2;
        return isSameRegion(x1, z1, x2, z2);
    }

    /**
     * Computes a high-entropy 32-bit hash code from region coordinates using an avalanche mixer.
     *
     * @param regionX region X coordinate
     * @param regionZ region Z coordinate
     * @return avalanched 32-bit hash code
     */
    public static int hashRegion(int regionX, int regionZ) {
        long packed = (((long) regionX) << 32) | (regionZ & INT_MASK);
        packed ^= packed >>> 33;
        packed *= PHI_MIXER;
        packed ^= packed >>> 29;
        return (int) (packed ^ (packed >>> 32));
    }

    /**
     * Calculates the Chebyshev distance (maximum coordinate difference) between two regions.
     *
     * @param regionKey1 first packed region key
     * @param regionKey2 second packed region key
     * @return Chebyshev distance in regions
     */
    public static int chebyshevDistance(long regionKey1, long regionKey2) {
        int x1 = unpackRegionX(regionKey1);
        int z1 = unpackRegionZ(regionKey1);
        int x2 = unpackRegionX(regionKey2);
        int z2 = unpackRegionZ(regionKey2);
        return Math.max(Math.abs(x1 - x2), Math.abs(z1 - z2));
    }

    /**
     * Calculates the Manhattan distance (sum of absolute coordinate differences) between two regions.
     *
     * @param regionKey1 first packed region key
     * @param regionKey2 second packed region key
     * @return Manhattan distance in regions
     */
    public static int manhattanDistance(long regionKey1, long regionKey2) {
        int x1 = unpackRegionX(regionKey1);
        int z1 = unpackRegionZ(regionKey1);
        int x2 = unpackRegionX(regionKey2);
        int z2 = unpackRegionZ(regionKey2);
        return Math.abs(x1 - x2) + Math.abs(z1 - z2);
    }
}
