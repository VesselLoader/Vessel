package dev.vessel.kernel.optimization.supermod;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Harmonizes uncapped modded armor values (&gt; 30 armor points from Apotheosis, Avaritia, Mekanism
 * MekaSuit, and AttributeFix) using a smooth asymptotic damage reduction curve, and bridges modded
 * shield blocks (Spartan Shields, Tinkers' Battlesigns) to Bukkit damage events.
 *
 * @version 1.0.0
 */
public final class UniversalCombatAndArmorHarmonizer {

    private final Map<UUID, Float> lastShieldAbsorbedDamage = new ConcurrentHashMap<>();

    /**
     * Calculates post-armor damage using an uncapped asymptotic curve so armor values exceeding
     * vanilla's 30.0 cap continue providing diminishing-returns protection up to 96% mitigation.
     *
     * @param incomingDamage   the raw incoming damage before armor
     * @param totalModdedArmor the entity's total modded armor points (may exceed 30)
     * @param armorToughness   the entity's total armor toughness
     * @return the reduced post-armor damage
     */
    public float calculateUncappedArmorDamageReduction(
            float incomingDamage,
            float totalModdedArmor,
            float armorToughness) {
        if (incomingDamage <= 0.0f) {
            return 0.0f;
        }
        float safeArmor = Math.max(0.0f, totalModdedArmor);
        float safeToughness = Math.max(0.0f, armorToughness);
        float effectiveDefense = safeArmor + (safeToughness * 0.5f);
        float reductionRatio = effectiveDefense / (effectiveDefense + 25.0f);
        float cappedReduction = Math.min(0.96f, Math.max(0.0f, reductionRatio));
        return incomingDamage * (1.0f - cappedReduction);
    }

    /**
     * Bridges a modded shield block action to Bukkit combat damage modifiers.
     *
     * @param defenderUuid          the defending entity's UUID
     * @param incomingDamage        the incoming attack damage
     * @param shieldAbsorptionRatio the fraction of damage absorbed by the modded shield (0.0 to 1.0)
     * @return {@code true} if the shield block absorbed positive damage and was recorded
     */
    public boolean bridgeModdedShieldBlockToBukkit(
            UUID defenderUuid,
            float incomingDamage,
            float shieldAbsorptionRatio) {
        Objects.requireNonNull(defenderUuid, "defenderUuid must not be null");
        if (incomingDamage <= 0.0f || shieldAbsorptionRatio <= 0.0f) {
            return false;
        }
        float clampedRatio = Math.min(1.0f, shieldAbsorptionRatio);
        lastShieldAbsorbedDamage.put(defenderUuid, incomingDamage * clampedRatio);
        return true;
    }

    /**
     * Returns the last recorded shield-absorbed damage for a defender.
     *
     * @param defenderUuid the defending entity's UUID
     * @return the absorbed damage amount, or {@code 0.0f} if none
     */
    public float getLastShieldAbsorbedDamage(UUID defenderUuid) {
        return lastShieldAbsorbedDamage.getOrDefault(defenderUuid, 0.0f);
    }
}
