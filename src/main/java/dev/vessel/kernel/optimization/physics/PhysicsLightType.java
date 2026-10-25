package dev.vessel.kernel.optimization.physics;

/**
 * Distinguishes between block-emitted light and sky-emitted light propagation types.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum PhysicsLightType {

    /**
     * Block lighting emitted by torches, glowstone, lava, lanterns, etc.
     */
    BLOCK(15),

    /**
     * Sky light emitted from the world ceiling and penetrating downwards through transparent columns.
     */
    SKY(15);

    private final int maxLightLevel;

    /**
     * Constructs a PhysicsLightType with maximum supported light level.
     *
     * @param maxLightLevel maximum intensity level
     */
    PhysicsLightType(int maxLightLevel) {
        this.maxLightLevel = maxLightLevel;
    }

    /**
     * Returns the maximum possible light level for this light channel.
     *
     * @return maximum light level (typically 15)
     */
    public int getMaxLightLevel() {
        return maxLightLevel;
    }
}
