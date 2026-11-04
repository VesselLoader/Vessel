package dev.vessel.kernel.gameplay.item;

/**
 * Immutable descriptor representing tool/weapon attribute modifiers (attack damage, attack speed, durability).
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public record ItemAttributeRecord(
        String itemIdentifier,
        double attackDamage,
        double attackSpeed,
        int maxDurability
) {
}
