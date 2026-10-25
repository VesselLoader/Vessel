package dev.vessel.kernel.optimization.lighting;

/**
 * Lighting propagation channel type.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum LightPropagationType {
    BLOCK(15),
    SKY(15);

    private final int maxLightLevel;

    LightPropagationType(int maxLightLevel) {
        this.maxLightLevel = maxLightLevel;
    }

    public int maxLightLevel() {
        return maxLightLevel;
    }
}
