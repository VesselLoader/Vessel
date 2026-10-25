package dev.vessel.kernel.modelengine.driver;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Immutable configuration for a custom item drop from a Model Engine driver mob.
 * Supports configurable count range, drop probability, and looting enchantment bonus.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class CustomDropItem {

    private final String itemId;
    private final int minCount;
    private final int maxCount;
    private final float dropChance;
    private final float lootingChanceBonus;
    private final int lootingCountBonus;

    /**
     * Constructs a CustomDropItem definition without looting count bonus.
     *
     * @param itemId             item namespaced identifier (e.g. "minecraft:diamond" or "mymod:dragon_scale")
     * @param minCount           minimum drop quantity (must be &gt;= 0)
     * @param maxCount           maximum drop quantity (must be &gt;= minCount)
     * @param dropChance         base drop probability [0.0, 1.0]
     * @param lootingChanceBonus extra drop chance per level of Looting enchantment
     */
    public CustomDropItem(String itemId, int minCount, int maxCount, float dropChance, float lootingChanceBonus) {
        this(itemId, minCount, maxCount, dropChance, lootingChanceBonus, 0);
    }

    /**
     * Constructs a CustomDropItem definition with looting count bonus.
     *
     * @param itemId             item namespaced identifier (e.g. "minecraft:diamond" or "mymod:dragon_scale")
     * @param minCount           minimum drop quantity (must be &gt;= 0)
     * @param maxCount           maximum drop quantity (must be &gt;= minCount)
     * @param dropChance         base drop probability [0.0, 1.0]
     * @param lootingChanceBonus extra drop chance per level of Looting enchantment
     * @param lootingCountBonus  extra maximum drop count per level of Looting enchantment
     */
    public CustomDropItem(String itemId, int minCount, int maxCount, float dropChance, float lootingChanceBonus, int lootingCountBonus) {
        this.itemId = Objects.requireNonNull(itemId, "itemId cannot be null");
        if (minCount < 0) {
            throw new IllegalArgumentException("minCount cannot be negative: " + minCount);
        }
        if (maxCount < minCount) {
            throw new IllegalArgumentException("maxCount cannot be less than minCount: " + maxCount + " < " + minCount);
        }
        if (dropChance < 0.0f || dropChance > 1.0f) {
            throw new IllegalArgumentException("dropChance must be within [0.0, 1.0]: " + dropChance);
        }
        if (lootingChanceBonus < 0.0f) {
            throw new IllegalArgumentException("lootingChanceBonus cannot be negative: " + lootingChanceBonus);
        }
        if (lootingCountBonus < 0) {
            throw new IllegalArgumentException("lootingCountBonus cannot be negative: " + lootingCountBonus);
        }
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.dropChance = dropChance;
        this.lootingChanceBonus = lootingChanceBonus;
        this.lootingCountBonus = lootingCountBonus;
    }

    /**
     * Determines the quantity dropped given a specific looting enchantment level.
     *
     * @param lootingLevel looting level (0 for none)
     * @return calculated drop quantity (0 if chance check fails)
     */
    public int rollDropCount(int lootingLevel) {
        float effectiveChance = Math.min(1.0f, dropChance + (Math.max(0, lootingLevel) * lootingChanceBonus));
        float roll = ThreadLocalRandom.current().nextFloat();
        if (roll > effectiveChance) {
            return 0;
        }
        int effectiveMax = maxCount + (Math.max(0, lootingLevel) * lootingCountBonus);
        if (minCount >= effectiveMax) {
            return minCount;
        }
        return ThreadLocalRandom.current().nextInt(minCount, effectiveMax + 1);
    }

    public String getItemId() {
        return itemId;
    }

    public int getMinCount() {
        return minCount;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public float getDropChance() {
        return dropChance;
    }

    public float getLootingChanceBonus() {
        return lootingChanceBonus;
    }

    public int getLootingCountBonus() {
        return lootingCountBonus;
    }
}
