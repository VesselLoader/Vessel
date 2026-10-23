package dev.vessel.kernel.modelengine.driver;

import java.util.Objects;

/**
 * Governs scalable custom max health and health modifications for Model Engine driver entities.
 * Enables setting custom boss/mob health values (e.g. 100 HP, 200 HP, 500 HP, 1000 HP)
 * while maintaining attribute bounds and ratio-preserving scaling.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class DriverHealthScaler {

    private static final float DEFAULT_BASE_HEALTH = 20.0f;

    private float baseMaxHealth;
    private float scalingMultiplier = 1.0f;

    /**
     * Constructs a DriverHealthScaler with default 20.0 HP.
     */
    public DriverHealthScaler() {
        this(DEFAULT_BASE_HEALTH);
    }

    /**
     * Constructs a DriverHealthScaler with a specific custom base max health.
     *
     * @param baseMaxHealth custom base max health (e.g. 100.0f, 200.0f, 500.0f)
     */
    public DriverHealthScaler(float baseMaxHealth) {
        if (baseMaxHealth <= 0.0f) {
            throw new IllegalArgumentException("baseMaxHealth must be positive: " + baseMaxHealth);
        }
        this.baseMaxHealth = baseMaxHealth;
    }

    /**
     * Applies the configured max health to the driver state, adjusting current health proportionally.
     *
     * @param state driver state to configure
     */
    public void configureDriverHealth(DriverMobState state) {
        Objects.requireNonNull(state, "state cannot be null");
        float effectiveMax = computeEffectiveMaxHealth();
        float previousMax = state.getMaxHealth();
        float healthRatio = (previousMax > 0.0f) ? (state.getCurrentHealth() / previousMax) : 1.0f;

        state.setMaxHealth(effectiveMax);
        state.setCurrentHealth(effectiveMax * healthRatio);
    }

    /**
     * Computes effective max health taking base max health and multiplier into account.
     *
     * @return effective maximum health
     */
    public float computeEffectiveMaxHealth() {
        return Math.max(1.0f, this.baseMaxHealth * this.scalingMultiplier);
    }

    /**
     * Calculates the normalized health percentage [0.0, 1.0] for boss bars or status displays.
     *
     * @param state driver state
     * @return health fraction between 0.0 and 1.0
     */
    public float getHealthPercentage(DriverMobState state) {
        Objects.requireNonNull(state, "state cannot be null");
        if (state.getMaxHealth() <= 0.0f) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, state.getCurrentHealth() / state.getMaxHealth()));
    }

    public float getBaseMaxHealth() {
        return baseMaxHealth;
    }

    public void setBaseMaxHealth(float baseMaxHealth) {
        if (baseMaxHealth <= 0.0f) {
            throw new IllegalArgumentException("baseMaxHealth must be positive: " + baseMaxHealth);
        }
        this.baseMaxHealth = baseMaxHealth;
    }

    public float getScalingMultiplier() {
        return scalingMultiplier;
    }

    public void setScalingMultiplier(float scalingMultiplier) {
        if (scalingMultiplier <= 0.0f) {
            throw new IllegalArgumentException("scalingMultiplier must be positive: " + scalingMultiplier);
        }
        this.scalingMultiplier = scalingMultiplier;
    }
}
