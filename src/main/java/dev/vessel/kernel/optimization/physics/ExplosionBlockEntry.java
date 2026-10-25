package dev.vessel.kernel.optimization.physics;

/**
 * Immutable spatial coordinate and state payload for a single block destroyed by an explosion.
 *
 * @param x               block world X coordinate
 * @param y               block world Y coordinate
 * @param z               block world Z coordinate
 * @param newBlockStateId target block state ID (typically 0 for air)
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record ExplosionBlockEntry(
        int x,
        int y,
        int z,
        int newBlockStateId
) {
    /**
     * Packs the 3D position into a single 64-bit coordinate key.
     *
     * @return 64-bit coordinate key
     */
    public long packKey() {
        return (((long) x & 0x3FFFFFFL) << 38) | (((long) y & 0xFFFL) << 26) | ((long) z & 0x3FFFFFFL);
    }
}
