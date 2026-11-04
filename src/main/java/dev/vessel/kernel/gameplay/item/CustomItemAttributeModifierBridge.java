package dev.vessel.kernel.gameplay.item;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Bridges custom weapon and tool attribute modifiers (attack damage, speed, armor)
 * to modern DataComponents ItemAttributeModifiers.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public final class CustomItemAttributeModifierBridge {

    private final ConcurrentMap<String, ItemAttributeRecord> attributeRegistry = new ConcurrentHashMap<>();

    public void registerItemAttributes(String itemIdentifier, double attackDamage, double attackSpeed, int maxDurability) {
        Objects.requireNonNull(itemIdentifier, "itemIdentifier cannot be null");
        ItemAttributeRecord record = new ItemAttributeRecord(itemIdentifier, attackDamage, attackSpeed, maxDurability);
        attributeRegistry.put(itemIdentifier, record);
    }

    public double getAttackDamage(String itemIdentifier) {
        ItemAttributeRecord record = attributeRegistry.get(itemIdentifier);
        return record != null ? record.attackDamage() : 1.0;
    }

    public double getAttackSpeed(String itemIdentifier) {
        ItemAttributeRecord record = attributeRegistry.get(itemIdentifier);
        return record != null ? record.attackSpeed() : 4.0;
    }

    public int getMaxDurability(String itemIdentifier) {
        ItemAttributeRecord record = attributeRegistry.get(itemIdentifier);
        return record != null ? record.maxDurability() : 100;
    }

    public boolean hasCustomAttributes(String itemIdentifier) {
        return itemIdentifier != null && attributeRegistry.containsKey(itemIdentifier);
    }
}
