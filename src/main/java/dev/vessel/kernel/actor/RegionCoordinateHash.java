package dev.vessel.kernel.actor;

/**
 * Immutable spatial coordinate hash for the Folia-compatible Regional Actor Concurrency Model.
 *
 * <p>Represents a coarse-grained world partition grouping a 16&times;16 cluster of chunks
 * (256&times;256 blocks) into an independent concurrency strand:
 * <pre>
 * RegionKey = (ChunkX &gt;&gt; 4, ChunkZ &gt;&gt; 4)
 * </pre>
 *
 * <p>Enables lockless dispatching of region tick tasks and fast spatial hashing for
 * cross-region message routing.</p>
 *
 * @param regionX the coarse region X coordinate
 * @param regionZ the coarse region Z coordinate
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-3)
 */
public record RegionCoordinateHash(int regionX, int regionZ) implements Comparable<RegionCoordinateHash> {

    /**
     * Constructs a RegionCoordinateHash from chunk coordinates.
     *
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return region coordinate hash
     */
    public static RegionCoordinateHash fromChunk(int chunkX, int chunkZ) {
        return new RegionCoordinateHash(chunkX >> 4, chunkZ >> 4);
    }

    /**
     * Constructs a RegionCoordinateHash from block coordinates.
     *
     * @param blockX block X coordinate
     * @param blockZ block Z coordinate
     * @return region coordinate hash
     */
    public static RegionCoordinateHash fromBlock(int blockX, int blockZ) {
        return new RegionCoordinateHash(blockX >> 8, blockZ >> 8);
    }

    /**
     * Packs the 2-dimensional 32-bit coordinates into a single 64-bit primitive long key.
     * Ideal for allocation-free hash maps and lock-free atomic tables.
     *
     * @return 64-bit packed region key
     */
    public long toPackedKey() {
        return (((long) regionX) << 32) | (regionZ & 0xFFFFFFFFL);
    }

    /**
     * Unpacks a 64-bit packed region key into a RegionCoordinateHash record.
     *
     * @param packedKey the packed 64-bit key
     * @return reconstructed RegionCoordinateHash instance
     */
    public static RegionCoordinateHash fromPackedKey(long packedKey) {
        int x = (int) (packedKey >> 32);
        int z = (int) packedKey;
        return new RegionCoordinateHash(x, z);
    }

    @Override
    public int compareTo(RegionCoordinateHash o) {
        int cmpX = Integer.compare(this.regionX, o.regionX);
        if (cmpX != 0) return cmpX;
        return Integer.compare(this.regionZ, o.regionZ);
    }

    @Override
    public String toString() {
        return "Region[" + regionX + ", " + regionZ + "]";
    }
}
