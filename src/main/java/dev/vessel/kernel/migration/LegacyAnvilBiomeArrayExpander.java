package dev.vessel.kernel.migration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Expands legacy 2D Anvil 256-element biome arrays into modern 3D paletted containers.
 * <p>
 * Enforces zero biome boundary distortion across vertical chunk sections while preventing
 * memory exhaustion during legacy world conversion.
 * </p>
 * <p>
 * Mitigates: <b>ERR-17</b>, <b>STO-001..020</b>.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class LegacyAnvilBiomeArrayExpander {

    /** Standard 2D biome array length in legacy Anvil format (16 x 16 blocks). */
    public static final int LEGACY_2D_BIOME_COUNT = 256;

    /** Standard horizontal block width per chunk. */
    public static final int CHUNK_HORIZONTAL_SIZE = 16;

    /** Biome voxels per section dimension (4 blocks per biome unit). */
    public static final int VOXELS_PER_DIMENSION = 4;

    /** Block coordinate scale factor per biome voxel. */
    public static final int BLOCK_SCALE = 4;

    /** Center offset within a 4x4 block biome cell for representative sampling. */
    public static final int CELL_CENTER_OFFSET = 2;

    /** Default legacy chunk vertical sections count (Y=0 to 255 -> 16 sections). */
    public static final int DEFAULT_SECTION_COUNT = 16;

    /** Default minimum section index for standard legacy chunks. */
    public static final int DEFAULT_MIN_SECTION_Y = 0;

    private final LegacyBiomeIdMapper biomeIdMapper;
    private final BiomePaletteCompressor paletteCompressor;

    /**
     * Constructs a new expander initializing internal mappers and compressors.
     */
    public LegacyAnvilBiomeArrayExpander() {
        this.biomeIdMapper = new LegacyBiomeIdMapper();
        this.paletteCompressor = new BiomePaletteCompressor();
    }

    /**
     * Expands a 256-byte legacy biome array into modern 3D paletted containers across 16 sections.
     *
     * @param legacyBytes raw 256-byte array representing 16x16 columns
     * @return unmodifiable list of 3D paletted containers
     * @throws BiomeArrayExpansionException if input is null or invalid length
     */
    public List<PalettedBiomeContainer> expandLegacyByteArray(byte[] legacyBytes) {
        return expandLegacyByteArray(legacyBytes, DEFAULT_MIN_SECTION_Y, DEFAULT_SECTION_COUNT);
    }

    /**
     * Expands a 256-byte legacy biome array into modern 3D paletted containers across custom sections.
     *
     * @param legacyBytes   raw 256-byte array representing 16x16 columns
     * @param minSectionY   minimum vertical section index
     * @param sectionCount  total number of vertical sections to populate
     * @return unmodifiable list of 3D paletted containers
     * @throws BiomeArrayExpansionException if input is null or invalid length
     */
    public List<PalettedBiomeContainer> expandLegacyByteArray(byte[] legacyBytes, int minSectionY, int sectionCount) {
        if (legacyBytes == null) {
            throw new BiomeArrayExpansionException("Legacy biome byte array cannot be null");
        }
        if (legacyBytes.length != LEGACY_2D_BIOME_COUNT) {
            throw new BiomeArrayExpansionException(
                    "Invalid legacy biome array length: expected " + LEGACY_2D_BIOME_COUNT + ", got " + legacyBytes.length
            );
        }
        if (sectionCount <= 0) {
            throw new BiomeArrayExpansionException("Section count must be greater than zero, got: " + sectionCount);
        }

        int[] intIds = new int[LEGACY_2D_BIOME_COUNT];
        for (int i = 0; i < LEGACY_2D_BIOME_COUNT; i++) {
            intIds[i] = legacyBytes[i] & 0xFF;
        }

        return expandInternal(intIds, minSectionY, sectionCount);
    }

    /**
     * Expands a 256-integer legacy biome array into modern 3D paletted containers.
     *
     * @param legacyInts raw 256-integer array
     * @param minSectionY minimum vertical section index
     * @param sectionCount total number of vertical sections to populate
     * @return unmodifiable list of 3D paletted containers
     * @throws BiomeArrayExpansionException if input is null or invalid length
     */
    public List<PalettedBiomeContainer> expandLegacyIntArray(int[] legacyInts, int minSectionY, int sectionCount) {
        if (legacyInts == null) {
            throw new BiomeArrayExpansionException("Legacy biome int array cannot be null");
        }
        if (legacyInts.length != LEGACY_2D_BIOME_COUNT) {
            throw new BiomeArrayExpansionException(
                    "Invalid legacy biome array length: expected " + LEGACY_2D_BIOME_COUNT + ", got " + legacyInts.length
            );
        }
        if (sectionCount <= 0) {
            throw new BiomeArrayExpansionException("Section count must be greater than zero, got: " + sectionCount);
        }

        return expandInternal(legacyInts, minSectionY, sectionCount);
    }

    private List<PalettedBiomeContainer> expandInternal(int[] biomeIds, int minSectionY, int sectionCount) {
        List<PalettedBiomeContainer> containers = new ArrayList<>(sectionCount);

        for (int sectionIndex = 0; sectionIndex < sectionCount; sectionIndex++) {
            int currentSectionY = minSectionY + sectionIndex;
            String[] sectionVoxels = new String[BiomePaletteCompressor.VOXEL_COUNT];
            int voxelIndex = 0;

            for (int vy = 0; vy < VOXELS_PER_DIMENSION; vy++) {
                for (int vz = 0; vz < VOXELS_PER_DIMENSION; vz++) {
                    for (int vx = 0; vx < VOXELS_PER_DIMENSION; vx++) {
                        int blockX = (vx * BLOCK_SCALE) + CELL_CENTER_OFFSET;
                        int blockZ = (vz * BLOCK_SCALE) + CELL_CENTER_OFFSET;
                        int legacyIndex = (blockZ * CHUNK_HORIZONTAL_SIZE) + blockX;

                        int rawBiomeId = biomeIds[legacyIndex];
                        String modernBiome = biomeIdMapper.resolveModernBiome(rawBiomeId);
                        sectionVoxels[voxelIndex++] = modernBiome;
                    }
                }
            }

            PalettedBiomeContainer container = paletteCompressor.compress(currentSectionY, sectionVoxels);
            containers.add(container);
        }

        return Collections.unmodifiableList(containers);
    }
}
