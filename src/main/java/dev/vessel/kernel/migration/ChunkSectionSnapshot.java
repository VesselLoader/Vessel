package dev.vessel.kernel.migration;

import java.util.Objects;

/**
 * Immutable snapshot of a single 16x16x16 chunk section.
 *
 * @param sectionY       vertical section index
 * @param biomes         paletted 3D biome container for this section
 * @param blockStateIds  array of 4096 palette state indices
 * @param blockLight     array of 2048 bytes encoding block light nibbles (or empty)
 * @param skyLight       array of 2048 bytes encoding sky light nibbles (or empty)
 * @param empty          flag indicating whether this section contains only air
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record ChunkSectionSnapshot(
        int sectionY,
        PalettedBiomeContainer biomes,
        short[] blockStateIds,
        byte[] blockLight,
        byte[] skyLight,
        boolean empty
) {

    /** Standard block count per 16x16x16 section. */
    public static final int BLOCKS_PER_SECTION = 4096;

    /** Standard light nibble array size (4096 / 2 = 2048 bytes). */
    public static final int LIGHT_ARRAY_SIZE = 2048;

    /**
     * Compact constructor enforcing immutability and defensive copying.
     */
    public ChunkSectionSnapshot {
        blockStateIds = blockStateIds != null ? blockStateIds.clone() : new short[0];
        blockLight = blockLight != null ? blockLight.clone() : new byte[0];
        skyLight = skyLight != null ? skyLight.clone() : new byte[0];
    }
}
