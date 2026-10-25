package dev.vessel.kernel.world.worldgen;

import java.util.Objects;

/**
 * Immutable block and lighting state snapshot served by phantom chunk views.
 * <p>
 * Directly resolves ERR-34 and WGN-0251..0500.
 * </p>
 *
 * @param x local chunk X coordinate (0..15)
 * @param y world Y coordinate (-64..320)
 * @param z local chunk Z coordinate (0..15)
 * @param blockId namespaced block identifier (e.g. "minecraft:air" or "minecraft:stone")
 * @param blockStateId internal integer block state protocol ID
 * @param lightLevel combined block/sky light level (0..15)
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record PhantomBlockSnapshot(
        int x,
        int y,
        int z,
        String blockId,
        int blockStateId,
        int lightLevel
) {

    /**
     * Standard synthetic air snapshot constant.
     */
    public static final PhantomBlockSnapshot AIR =
            new PhantomBlockSnapshot(0, 0, 0, "minecraft:air", 0, 0);

    /**
     * Standard synthetic stone snapshot constant.
     */
    public static final PhantomBlockSnapshot STONE =
            new PhantomBlockSnapshot(0, 0, 0, "minecraft:stone", 1, 0);

    /**
     * Compact constructor validating invariants.
     */
    public PhantomBlockSnapshot {
        Objects.requireNonNull(blockId, "blockId cannot be null");
        if (lightLevel < 0 || lightLevel > 15) {
            throw new IllegalArgumentException("lightLevel must be 0..15: " + lightLevel);
        }
    }
}
