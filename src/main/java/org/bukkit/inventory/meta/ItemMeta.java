package org.bukkit.inventory.meta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;

/**
 * Represents mutable custom display metadata (`displayName`, `lore`, `customModelData`),
 * enchantments, and persistent plugin data attached to an {@link org.bukkit.inventory.ItemStack}
 * on the VESSEL Sovereign Microkernel.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public final class ItemMeta implements PersistentDataHolder {

    private String displayName;
    private List<String> lore = new ArrayList<>();
    private Integer customModelData;
    private final Map<Enchantment, Integer> enchants = new ConcurrentHashMap<>();
    private PersistentDataContainer persistentDataContainer = new PersistentDataContainer();

    /**
     * Constructs an empty {@code ItemMeta}.
     */
    public ItemMeta() {
    }

    /**
     * Checks whether a non-empty custom display name is set.
     *
     * @return {@code true} if a display name is present
     */
    public boolean hasDisplayName() {
        return displayName != null && !displayName.isEmpty();
    }

    /**
     * Returns the custom display name, or {@code null} if unset.
     *
     * @return custom display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the custom display name.
     *
     * @param name custom display name (nullable)
     */
    public void setDisplayName(String name) {
        this.displayName = name;
    }

    /**
     * Checks whether non-empty lore lines are set.
     *
     * @return {@code true} if lore is present
     */
    public boolean hasLore() {
        return lore != null && !lore.isEmpty();
    }

    /**
     * Returns an unmodifiable view of the lore lines.
     *
     * @return unmodifiable list of lore lines
     */
    public List<String> getLore() {
        return Collections.unmodifiableList(lore);
    }

    /**
     * Replaces the lore lines with a defensive copy of the supplied list.
     *
     * @param lore lore lines (nullable)
     */
    public void setLore(List<String> lore) {
        this.lore = lore != null ? new ArrayList<>(lore) : new ArrayList<>();
    }

    /**
     * Checks whether a custom model data integer is set.
     *
     * @return {@code true} if custom model data is present
     */
    public boolean hasCustomModelData() {
        return customModelData != null;
    }

    /**
     * Returns the custom model data integer, or {@code 0} if unset.
     *
     * @return custom model data value
     */
    public int getCustomModelData() {
        return customModelData != null ? customModelData : 0;
    }

    /**
     * Sets the custom model data integer.
     *
     * @param data custom model data value (nullable)
     */
    public void setCustomModelData(Integer data) {
        this.customModelData = data;
    }

    public boolean hasEnchants() {
        return !enchants.isEmpty();
    }

    public boolean hasEnchant(Enchantment ench) {
        return ench != null && enchants.containsKey(ench);
    }

    public int getEnchantLevel(Enchantment ench) {
        return ench != null ? enchants.getOrDefault(ench, 0) : 0;
    }

    public Map<Enchantment, Integer> getEnchants() {
        return Collections.unmodifiableMap(enchants);
    }

    public boolean addEnchant(Enchantment ench, int level, boolean ignoreLevelRestriction) {
        Objects.requireNonNull(ench, "ench must not be null");
        if (!ignoreLevelRestriction && (level < ench.getStartLevel() || level > ench.getMaxLevel())) {
            return false;
        }
        enchants.put(ench, level);
        return true;
    }

    public boolean removeEnchant(Enchantment ench) {
        return ench != null && enchants.remove(ench) != null;
    }

    @Override
    public PersistentDataContainer getPersistentDataContainer() {
        return persistentDataContainer;
    }

    /**
     * Creates a deep copy of this {@code ItemMeta} instance.
     *
     * @return deep-copied {@code ItemMeta}
     */
    public ItemMeta copy() {
        ItemMeta clone = new ItemMeta();
        clone.displayName = this.displayName;
        clone.lore = this.lore != null ? new ArrayList<>(this.lore) : new ArrayList<>();
        clone.customModelData = this.customModelData;
        clone.enchants.putAll(this.enchants);
        clone.persistentDataContainer = this.persistentDataContainer.copy();
        return clone;
    }
}
