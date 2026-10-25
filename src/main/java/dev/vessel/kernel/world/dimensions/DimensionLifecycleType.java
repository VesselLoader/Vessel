package dev.vessel.kernel.world.dimensions;

/**
 * Dimension environment classifications for dynamic world creation and routing.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum DimensionLifecycleType {

    /**
     * Standard surface Overworld environment.
     */
    OVERWORLD,

    /**
     * Nether hell environment with ceiling bedrock and lava ocean.
     */
    NETHER,

    /**
     * The End dimension with void floor and dragon fight logic.
     */
    THE_END,

    /**
     * Custom modded dimension (e.g. Mining World, Compact Machine, Aether).
     */
    CUSTOM_MODDED
}
