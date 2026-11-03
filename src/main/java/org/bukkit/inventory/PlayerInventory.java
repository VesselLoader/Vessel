package org.bukkit.inventory;

/**
 * Runtime representation of a player's 36-slot main inventory, hotbar selection, off-hand slot,
 * and 4 armor equipment slots ({@code org.bukkit.inventory.PlayerInventory}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class PlayerInventory extends Inventory {

    private int heldItemSlot = 0;
    private ItemStack offHand;
    private ItemStack helmet;
    private ItemStack chestplate;
    private ItemStack leggings;
    private ItemStack boots;

    /**
     * Constructs a new 36-slot {@code PlayerInventory}.
     */
    public PlayerInventory() {
        super(36, "PlayerInventory");
    }

    public int getHeldItemSlot() {
        return heldItemSlot;
    }

    /**
     * Sets the active hotbar slot index ({@code 0..8}).
     *
     * @param slot hotbar slot index
     */
    public void setHeldItemSlot(int slot) {
        if (slot < 0 || slot >= 9) {
            throw new IllegalArgumentException("Hotbar slot must be between 0 and 8: " + slot);
        }
        this.heldItemSlot = slot;
    }

    public ItemStack getItemInMainHand() {
        return getItem(heldItemSlot);
    }

    public void setItemInMainHand(ItemStack item) {
        setItem(heldItemSlot, item);
    }

    public ItemStack getItemInOffHand() {
        return offHand;
    }

    public void setItemInOffHand(ItemStack offHand) {
        this.offHand = offHand;
    }

    public ItemStack getHelmet() {
        return helmet;
    }

    public void setHelmet(ItemStack helmet) {
        this.helmet = helmet;
    }

    public ItemStack getChestplate() {
        return chestplate;
    }

    public void setChestplate(ItemStack chestplate) {
        this.chestplate = chestplate;
    }

    public ItemStack getLeggings() {
        return leggings;
    }

    public void setLeggings(ItemStack leggings) {
        this.leggings = leggings;
    }

    public ItemStack getBoots() {
        return boots;
    }

    public void setBoots(ItemStack boots) {
        this.boots = boots;
    }
}
