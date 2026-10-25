package dev.vessel.kernel.display.model;

/**
 * Operating temperature stages of the Blood Cauldron (Iron's Spells and Spellbooks).
 * Affects fluid boiling volatility, particle emission frequency, and spellcrafting readiness.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum CauldronTemperatureState {

    /**
     * Stagnant cold blood, minimal or no surface motion.
     */
    COLD(0.0f, 0x5C0303),

    /**
     * Warm blood, gentle ripples.
     */
    WARM(0.35f, 0x8A0303),

    /**
     * Simmering blood, small bubbles beginning to rise.
     */
    SIMMERING(0.70f, 0xAF0505),

    /**
     * Active rolling boil, dense crimson steam and blood splashes.
     */
    BOILING(1.0f, 0xD41111),

    /**
     * Overheated blood, violent turbulence and danger of arcane explosion.
     */
    OVERHEATED(1.4f, 0xFF2A2A);

    private final float temperatureFactor;
    private final int fluidColorHex;

    CauldronTemperatureState(float temperatureFactor, int fluidColorHex) {
        this.temperatureFactor = temperatureFactor;
        this.fluidColorHex = fluidColorHex;
    }

    public float getTemperatureFactor() {
        return temperatureFactor;
    }

    public int getFluidColorHex() {
        return fluidColorHex;
    }
}
