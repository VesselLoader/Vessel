package com.example.mod.pure;

import dev.vessel.api.item.ItemBurnTimeTable;
import dev.vessel.api.item.ItemDurabilityTransmutator;

import java.util.Objects;

/**
 * Demonstrates high-speed item handling, durability management, and fuel burn lookup
 * using VESSEL Super-API.
 *
 * <p>Seamlessly unifies 1.20.5+ DataComponents with Bukkit ItemMeta and Forge ItemStack capabilities
 * without allocating wrapper objects during hot-path combat or inventory transfers.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 * @since 1.0.0
 */
public final class PlasmaBladeItemManager {

    /**
     * Constructs a new plasma blade item manager.
     */
    public PlasmaBladeItemManager() {
    }

    /**
     * Applies durability damage to an item stack and checks if the item is broken.
     *
     * @param itemStack item stack handle
     * @param damage    amount of durability damage
     * @return {@code true} if the item completely broke
     */
    public boolean damageBlade(Object itemStack, int damage) {
        Objects.requireNonNull(itemStack, "itemStack cannot be null");
        return ItemDurabilityTransmutator.damageItem(itemStack, damage, null);
    }

    /**
     * Retrieves current damage of an item stack.
     *
     * @param itemStack item stack handle
     * @return current damage value
     */
    public int getBladeDamage(Object itemStack) {
        Objects.requireNonNull(itemStack, "itemStack cannot be null");
        return ItemDurabilityTransmutator.getDamage(itemStack);
    }

    /**
     * Evaluates whether an item can be used as furnace fuel and retrieves its burn time in ticks.
     *
     * @param itemId numeric item ID
     * @return burn time in ticks (< 1.0 ns flat array lookup)
     */
    public int getFuelBurnTime(int itemId) {
        return ItemBurnTimeTable.getBurnTime(itemId);
    }

    /**
     * Repairs an item stack by reducing its damage value.
     *
     * @param itemStack    item stack handle
     * @param repairAmount points of durability restored
     */
    public void repairBlade(Object itemStack, int repairAmount) {
        Objects.requireNonNull(itemStack, "itemStack cannot be null");
        ItemDurabilityTransmutator.repairItem(itemStack, repairAmount);
    }
}
