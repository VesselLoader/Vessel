package dev.vessel.kernel.migration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Compressor encoding 64 3D biome voxels into a bit-packed 64-bit word palette container.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class BiomePaletteCompressor {

    /** Total biome voxels in a 16x16x16 section (4x4x4). */
    public static final int VOXEL_COUNT = 64;

    /** Single-valued palette bit width. */
    public static final int ZERO_BITS = 0;

    /**
     * Compresses an array of 64 biome names into a compact {@link PalettedBiomeContainer}.
     *
     * @param sectionY the vertical section index
     * @param voxels   array of exactly 64 biome names in YZX index order
     * @return the packed paletted biome container
     */
    public PalettedBiomeContainer compress(int sectionY, String[] voxels) {
        Objects.requireNonNull(voxels, "voxels cannot be null");
        if (voxels.length != VOXEL_COUNT) {
            throw new IllegalArgumentException("Expected exactly " + VOXEL_COUNT + " voxels, but received " + voxels.length);
        }

        Map<String, Integer> paletteMap = new LinkedHashMap<>();
        int[] indices = new int[VOXEL_COUNT];

        for (int i = 0; i < VOXEL_COUNT; i++) {
            String biome = voxels[i] != null ? voxels[i] : LegacyBiomeIdMapper.DEFAULT_FALLBACK_BIOME;
            Integer existingIndex = paletteMap.get(biome);
            if (existingIndex == null) {
                int newIndex = paletteMap.size();
                paletteMap.put(biome, newIndex);
                indices[i] = newIndex;
            } else {
                indices[i] = existingIndex;
            }
        }

        List<String> paletteList = new ArrayList<>(paletteMap.keySet());
        int paletteSize = paletteList.size();

        if (paletteSize <= 1) {
            return new PalettedBiomeContainer(sectionY, paletteList, new long[0], ZERO_BITS);
        }

        int bitsPerEntry = calculateBitsPerEntry(paletteSize);
        int entriesPerLong = Long.SIZE / bitsPerEntry;
        int longArrayLength = (VOXEL_COUNT + entriesPerLong - 1) / entriesPerLong;
        long[] packedData = new long[longArrayLength];

        for (int i = 0; i < VOXEL_COUNT; i++) {
            int longIndex = i / entriesPerLong;
            int bitOffset = (i % entriesPerLong) * bitsPerEntry;
            long value = (long) indices[i] << bitOffset;
            packedData[longIndex] |= value;
        }

        return new PalettedBiomeContainer(sectionY, paletteList, packedData, bitsPerEntry);
    }

    /**
     * Computes the minimal bit width required to address elements in a palette.
     *
     * @param paletteSize number of distinct entries
     * @return required bits per entry (1 to 6)
     */
    public int calculateBitsPerEntry(int paletteSize) {
        if (paletteSize <= 1) {
            return 0;
        }
        int bits = 1;
        while ((1 << bits) < paletteSize) {
            bits++;
        }
        return Math.min(bits, 6);
    }
}
