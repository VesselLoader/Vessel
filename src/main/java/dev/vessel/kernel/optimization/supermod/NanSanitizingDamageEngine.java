package dev.vessel.kernel.optimization.supermod;

/**
 * Sanitizes {@link Float#NaN} and {@link Float#POSITIVE_INFINITY} / {@link Float#NEGATIVE_INFINITY}
 * damage and health values produced by god-tier weapons and armor (Avaritia Sword of the Cosmos,
 * ProjectE Katar, Draconic Evolution Chaos Guardian) to prevent permanent entity corruption and
 * server crashes.
 *
 * @version 1.0.0
 */
public final class NanSanitizingDamageEngine {

    /**
     * Sanitizes a raw damage value into a finite non-negative float bounded by
     * {@code maxAllowedCap}.
     *
     * @param rawDamage     the incoming raw damage (may be {@code NaN} or {@code Infinity})
     * @param maxAllowedCap the maximum finite damage cap allowed by the kernel
     * @return a finite damage value in {@code [0.0f, maxAllowedCap]}
     */
    public float sanitizeDamageValue(float rawDamage, float maxAllowedCap) {
        float safeCap = Float.isFinite(maxAllowedCap) && maxAllowedCap > 0.0f
                ? maxAllowedCap
                : 1_000_000.0f;
        if (Float.isNaN(rawDamage) || rawDamage < 0.0f) {
            return 0.0f;
        }
        if (Float.isInfinite(rawDamage) || rawDamage > safeCap) {
            return safeCap;
        }
        return rawDamage;
    }

    /**
     * Sanitizes an entity's health value so {@code NaN} or infinite health values never corrupt
     * entity NBT or trigger the vanilla "NaN Health Entity Crash".
     *
     * @param rawHealth the entity's raw health value
     * @param maxHealth the entity's maximum health attribute
     * @return a finite health value in {@code [0.0f, safeMaxHealth]}
     */
    public float sanitizeEntityHealth(float rawHealth, float maxHealth) {
        float safeMax = Float.isFinite(maxHealth) && maxHealth > 0.0f ? maxHealth : 20.0f;
        if (Float.isNaN(rawHealth)) {
            return safeMax;
        }
        if (rawHealth == Float.NEGATIVE_INFINITY || rawHealth < 0.0f) {
            return 0.0f;
        }
        if (rawHealth == Float.POSITIVE_INFINITY || rawHealth > safeMax) {
            return safeMax;
        }
        return rawHealth;
    }
}
