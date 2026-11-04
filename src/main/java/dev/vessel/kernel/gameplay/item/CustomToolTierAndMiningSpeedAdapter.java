package dev.vessel.kernel.gameplay.item;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Calculates mining speed multipliers and harvest level tiers for modded custom tools.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public final class CustomToolTierAndMiningSpeedAdapter {

    private final ConcurrentMap<String, Float> toolMiningSpeeds = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Integer> toolMiningLevels = new ConcurrentHashMap<>();

    public void registerToolTier(String toolIdentifier, float miningSpeedMultiplier, int harvestTierLevel) {
        Objects.requireNonNull(toolIdentifier, "toolIdentifier cannot be null");
        toolMiningSpeeds.put(toolIdentifier, Math.max(1.0f, miningSpeedMultiplier));
        toolMiningLevels.put(toolIdentifier, Math.max(0, harvestTierLevel));
    }

    public float getMiningSpeedMultiplier(String toolIdentifier) {
        if (toolIdentifier == null) {
            return 1.0f;
        }
        return toolMiningSpeeds.getOrDefault(toolIdentifier, 1.0f);
    }

    public int getHarvestTierLevel(String toolIdentifier) {
        if (toolIdentifier == null) {
            return 0;
        }
        return toolMiningLevels.getOrDefault(toolIdentifier, 0);
    }

    public boolean canHarvestBlockTier(String toolIdentifier, int requiredTier) {
        int toolTier = getHarvestTierLevel(toolIdentifier);
        return toolTier >= requiredTier;
    }
}
