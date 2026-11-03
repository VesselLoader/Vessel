package net.fabricmc.fabric.api.biome.v1;

/**
 * Canonical Fabric Biome API v1 ordered biome modification phases
 * ({@code net.fabricmc.fabric.api.biome.v1.ModificationPhase}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public enum ModificationPhase {
    /**
     * Feature, carver, and spawn additions.
     */
    ADDITIONS,

    /**
     * Feature, carver, and spawn removals.
     */
    REMOVALS,

    /**
     * Feature and property replacements.
     */
    REPLACEMENTS,

    /**
     * Final post-processing adjustments after all additions/removals/replacements.
     */
    POST_PROCESSING
}
