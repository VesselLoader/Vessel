package dev.vessel.kernel.migration;

import java.util.List;
import java.util.Objects;

/**
 * Immutable 3D paletted biome container representing a single 16x16x16 chunk section
 * (partitioned into 4x4x4 biome voxels, totaling 64 entries).
 *
 * @param sectionY      the Y section index (e.g. 0 to 15, or -4 to 19 in modern Minecraft)
 * @param palette       immutable list of unique biome namespaced IDs
 * @param packedData    bit-packed indices array encoding the 64 biome voxels
 * @param bitsPerEntry  the number of bits allocated per voxel index (0 if single-valued)
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record PalettedBiomeContainer(
        int sectionY,
        List<String> palette,
        long[] packedData,
        int bitsPerEntry
) {

    /** Expected total biome voxels per 16x16x16 section (4 x 4 x 4). */
    public static final int VOXELS_PER_SECTION = 64;

    /** Voxel coordinate dimension bound (4 blocks along each axis). */
    public static final int VOXEL_SIZE = 4;

    /**
     * Compact constructor validating invariants.
     */
    public PalettedBiomeContainer {
        Objects.requireNonNull(palette, "palette cannot be null");
        Objects.requireNonNull(packedData, "packedData cannot be null");
        if (palette.isEmpty()) {
            throw new IllegalArgumentException("palette cannot be empty");
        }
        palette = List.copyOf(palette);
        packedData = packedData.clone();
    }

    /**
     * Retrieves the biome identifier at a specific 4x4x4 section voxel coordinate.
     *
     * @param voxelX X coordinate in voxel space (0 to 3)
     * @param voxelY Y coordinate in voxel space (0 to 3)
     * @param voxelZ Z coordinate in voxel space (0 to 3)
     * @return the namespaced biome identifier
     */
    public String getBiome(int voxelX, int voxelY, int voxelZ) {
        if (voxelX < 0 || voxelX >= VOXEL_SIZE || voxelY < 0 || voxelY >= VOXEL_SIZE || voxelZ < 0 || voxelZ >= VOXEL_SIZE) {
            throw new IndexOutOfBoundsException("Voxel coordinates must be in range [0, 3]");
        }

        if (bitsPerEntry == 0 || palette.size() == 1) {
            return palette.getFirst();
        }

        int index = (voxelY * VOXEL_SIZE + voxelZ) * VOXEL_SIZE + voxelX;
        int entriesPerLong = Long.SIZE / bitsPerEntry;
        int longIndex = index / entriesPerLong;
        int bitOffset = (index % entriesPerLong) * bitsPerEntry;
        long mask = (1L << bitsPerEntry) - 1L;

        if (longIndex >= packedData.length) {
            return palette.getFirst();
        }

        int paletteIndex = (int) ((packedData[longIndex] >>> bitOffset) & mask);
        if (paletteIndex < 0 || paletteIndex >= palette.size()) {
            return palette.getFirst();
        }
        return palette.get(paletteIndex);
    }
}
