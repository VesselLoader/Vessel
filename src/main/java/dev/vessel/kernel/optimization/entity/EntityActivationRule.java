package dev.vessel.kernel.optimization.entity;

/**
 * Customization rule governing entity activation exemptions (e.g. boss mobs or vehicles).
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
@FunctionalInterface
public interface EntityActivationRule {

    /**
     * Determines whether an entity type should be forced into a specific activation tier.
     *
     * @param entityType namespaced entity type (e.g., "minecraft:ender_dragon")
     * @return overridden tier, or null to follow standard radial distance rules
     */
    EntityActivationTier resolveOverride(String entityType);
}
