package dev.vessel.kernel.world.worldgen;

/**
 * Read-only abstraction layer querying existing in-memory chunk data without triggering generation tickets.
 * <p>
 * Directly resolves ERR-34 and WGN-0251..0500.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public interface PhantomChunkProvider {

    /**
     * Checks if the given chunk coordinate is currently fully loaded in memory.
     *
     * @param chunkKey packed 64-bit chunk coordinate
     * @return {@code true} if loaded in RAM
     */
    boolean isChunkLoaded(long chunkKey);

    /**
     * Reads a block snapshot from a loaded chunk.
     *
     * @param chunkKey packed 64-bit chunk coordinate
     * @param localX block X within chunk (0..15)
     * @param y block Y coordinate
     * @param localZ block Z within chunk (0..15)
     * @return block snapshot, or {@code null} if chunk is not in memory
     */
    PhantomBlockSnapshot getBlock(long chunkKey, int localX, int y, int localZ);

    /**
     * Reads the biome ID at the given position.
     *
     * @param chunkKey packed 64-bit chunk coordinate
     * @param localX block X within chunk (0..15)
     * @param y block Y coordinate
     * @param localZ block Z within chunk (0..15)
     * @return biome ID, or -1 if chunk is not in memory
     */
    int getBiomeId(long chunkKey, int localX, int y, int localZ);

    /**
     * Reads the terrain height at the given column.
     *
     * @param chunkKey packed 64-bit chunk coordinate
     * @param localX block X within chunk (0..15)
     * @param localZ block Z within chunk (0..15)
     * @return highest non-air block Y coordinate
     */
    int getHeight(long chunkKey, int localX, int localZ);
}
