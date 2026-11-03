package org.bukkit.inventory;

import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Runtime representation of an inventory item stack ({@code org.bukkit.inventory.ItemStack}) on the
 * VESSEL Sovereign Microkernel.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ItemStack {

    private Material type;
    private int amount;
    private ItemMeta itemMeta;

    /**
     * Constructs a single-item {@code ItemStack} of the specified {@link Material}.
     *
     * @param type non-null item material
     */
    public ItemStack(Material type) {
        this(type, 1);
    }

    /**
     * Constructs an {@code ItemStack} of the specified {@link Material} and stack count.
     *
     * @param type   non-null item material
     * @param amount stack size
     */
    public ItemStack(Material type, int amount) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.amount = amount;
    }

    public Material getType() {
        return type;
    }

    public void setType(Material type) {
        this.type = Objects.requireNonNull(type, "type must not be null");
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    /**
     * Checks whether this item stack carries non-empty custom {@link ItemMeta}.
     *
     * @return {@code true} if custom item metadata is present
     */
    public boolean hasItemMeta() {
        return itemMeta != null
                && (itemMeta.hasDisplayName() || itemMeta.hasLore() || itemMeta.hasCustomModelData());
    }

    /**
     * Returns a defensive copy of this item stack's {@link ItemMeta}, initializing a fresh instance
     * if none was previously attached.
     *
     * @return item metadata copy
     */
    public ItemMeta getItemMeta() {
        if (itemMeta == null) {
            itemMeta = new ItemMeta();
        }
        return itemMeta.copy();
    }

    /**
     * Attaches a defensive copy of the supplied {@link ItemMeta} to this item stack.
     *
     * @param meta item metadata to attach (nullable)
     * @return {@code true} after updating metadata
     */
    public boolean setItemMeta(ItemMeta meta) {
        this.itemMeta = meta != null ? meta.copy() : null;
        return true;
    }

    /**
     * Checks whether the supplied item stack shares the same {@link Material} type.
     *
     * @param other other item stack to compare (nullable)
     * @return {@code true} if similar
     */
    public boolean isSimilar(ItemStack other) {
        return other != null && this.type == other.type;
    }
}
