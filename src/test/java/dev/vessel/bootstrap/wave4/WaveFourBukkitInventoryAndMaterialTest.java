package dev.vessel.bootstrap.wave4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.VesselWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Wave 4 verifying Bukkit {@link NamespacedKey}, {@link Material},
 * {@link ItemStack}, {@link ItemMeta}, {@link Inventory}, {@link PlayerInventory},
 * {@link InventoryClickEvent}, and {@link InventoryCloseEvent}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveFourBukkitInventoryAndMaterialTest {

    @Test
    void testMaterialNamespacedKeyItemStackPlayerInventoryAndEvents() {
        NamespacedKey mcSwordKey = NamespacedKey.minecraft("diamond_sword");
        assertEquals("minecraft", mcSwordKey.getNamespace());
        assertEquals("diamond_sword", mcSwordKey.getKey());
        assertEquals("minecraft:diamond_sword", mcSwordKey.toString());

        NamespacedKey customWandKey = NamespacedKey.fromString("vessel:custom_wand");
        assertNotNull(customWandKey);
        assertEquals("vessel", customWandKey.getNamespace());
        assertEquals("custom_wand", customWandKey.getKey());

        Material matched = Material.matchMaterial("minecraft:diamond_sword");
        assertEquals(Material.DIAMOND_SWORD, matched);
        assertEquals(1, Material.DIAMOND_SWORD.getMaxStackSize());
        assertFalse(Material.DIAMOND_SWORD.isBlock());
        assertTrue(Material.DIAMOND_SWORD.isItem());
        assertTrue(Material.AIR.isAir());

        ItemStack excalibur = new ItemStack(Material.DIAMOND_SWORD, 1);
        assertFalse(excalibur.hasItemMeta());
        ItemMeta meta = excalibur.getItemMeta();
        meta.setDisplayName("Excalibur");
        meta.setLore(List.of("Tier 5"));
        meta.setCustomModelData(42);
        assertTrue(excalibur.setItemMeta(meta));
        assertTrue(excalibur.hasItemMeta());

        ItemMeta readBack = excalibur.getItemMeta();
        assertEquals("Excalibur", readBack.getDisplayName());
        assertEquals(List.of("Tier 5"), readBack.getLore());
        assertEquals(42, readBack.getCustomModelData());

        World world = new VesselWorld("world");
        Player player = new Player("Arthur", new Location(world, 0.0, 64.0, 0.0));
        PlayerInventory inv = player.getInventory();
        assertNotNull(inv);
        assertEquals(36, inv.getSize());

        inv.setItemInMainHand(excalibur);
        assertEquals(Material.DIAMOND_SWORD, inv.getItemInMainHand().getType());
        assertTrue(inv.contains(Material.DIAMOND_SWORD));

        ItemStack crown = new ItemStack(Material.GOLD_INGOT, 1);
        inv.setHelmet(crown);
        assertEquals(Material.GOLD_INGOT, inv.getHelmet().getType());

        inv.addItem(new ItemStack(Material.GOLDEN_APPLE, 8));
        assertTrue(inv.contains(Material.GOLDEN_APPLE));

        Inventory chestGui = new Inventory(27, "Vault Chest");
        chestGui.setItem(0, excalibur);
        InventoryClickEvent clickEvent = new InventoryClickEvent(chestGui, player, 0, excalibur);
        assertEquals(0, clickEvent.getSlot());
        assertEquals(player, clickEvent.getWhoClicked());
        assertEquals(excalibur, clickEvent.getCurrentItem());
        assertFalse(clickEvent.isCancelled());
        clickEvent.setCancelled(true);
        assertTrue(clickEvent.isCancelled());

        InventoryCloseEvent closeEvent = new InventoryCloseEvent(chestGui, player);
        assertEquals(chestGui, closeEvent.getInventory());
        assertEquals(player, closeEvent.getPlayer());
    }
}
