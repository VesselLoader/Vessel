package dev.vessel.bootstrap.wave10;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vessel.api.storage.VesselVirtualInventorySnapshot;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.VesselWorld;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Wave 10 verifying Bukkit {@link Bukkit#createInventory},
 * {@link InventoryType}, {@link EquipmentSlot}, {@link ClickType}, {@link InventoryOpenEvent},
 * {@link EntityDeathEvent}, {@link PlayerDeathEvent}, {@link PlayerRespawnEvent},
 * {@link PlayerCommandPreprocessEvent}, {@link ServerCommandEvent}, and VUSA
 * {@link VesselVirtualInventorySnapshot}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveTenBukkitInventoryGuiAndDeathRespawnTest {

    @Test
    void testBukkitCustomInventoryGuiEventsDeathRespawnAndVusaSnapshot() {
        Inventory hopperGui =
                Bukkit.createInventory(null, InventoryType.HOPPER, "Custom Hopper");
        assertNotNull(hopperGui);
        assertNull(hopperGui.getHolder());
        assertEquals(InventoryType.HOPPER, hopperGui.getType());
        assertEquals(5, hopperGui.getSize());
        assertEquals("Custom Hopper", hopperGui.getTitle());

        hopperGui.setItem(0, new ItemStack(Material.DIAMOND, 4));
        hopperGui.setItem(2, new ItemStack(Material.EMERALD, 1));

        assertEquals(EquipmentSlot.HAND, EquipmentSlot.valueOf("HAND"));
        assertTrue(ClickType.SHIFT_LEFT.isLeftClick());
        assertTrue(ClickType.SHIFT_LEFT.isShiftClick());
        assertFalse(ClickType.SHIFT_LEFT.isRightClick());
        assertTrue(ClickType.RIGHT.isRightClick());

        World world = new VesselWorld("world");
        Location spawnLoc = new Location(world, 0.0D, 64.0D, 0.0D);
        Player player = new Player("Shiroz", spawnLoc);

        InventoryOpenEvent openEvent = new InventoryOpenEvent(hopperGui, player);
        assertEquals(hopperGui, openEvent.getInventory());
        assertEquals(player, openEvent.getPlayer());
        assertFalse(openEvent.isCancelled());
        openEvent.setCancelled(true);
        assertTrue(openEvent.isCancelled());

        Entity zombie = world.spawnEntity(spawnLoc, EntityType.ZOMBIE);
        EntityDeathEvent entityDeath = new EntityDeathEvent(
                zombie,
                List.of(new ItemStack(Material.IRON_INGOT, 1)),
                5);
        assertEquals(zombie, entityDeath.getEntity());
        assertEquals(1, entityDeath.getDrops().size());
        assertEquals(5, entityDeath.getDroppedExp());
        entityDeath.setDroppedExp(12);
        assertEquals(12, entityDeath.getDroppedExp());

        PlayerDeathEvent playerDeath = new PlayerDeathEvent(
                player,
                List.of(new ItemStack(Material.DIAMOND_SWORD, 1)),
                100,
                "Shiroz fell from a high place");
        assertEquals(player, playerDeath.getEntity());
        assertEquals("Shiroz fell from a high place", playerDeath.getDeathMessage());
        playerDeath.setKeepInventory(true);
        playerDeath.setKeepLevel(true);
        assertTrue(playerDeath.getKeepInventory());
        assertTrue(playerDeath.getKeepLevel());

        Location bedLoc = new Location(world, 128.0D, 70.0D, -64.0D);
        PlayerRespawnEvent respawnEvent = new PlayerRespawnEvent(player, bedLoc, true, false);
        assertEquals(player, respawnEvent.getPlayer());
        assertEquals(bedLoc, respawnEvent.getRespawnLocation());
        assertTrue(respawnEvent.isBedSpawn());
        assertFalse(respawnEvent.isAnchorSpawn());

        PlayerCommandPreprocessEvent preprocessEvent =
                new PlayerCommandPreprocessEvent(player, "/spawn");
        assertEquals(player, preprocessEvent.getPlayer());
        assertEquals("/spawn", preprocessEvent.getMessage());
        preprocessEvent.setMessage("/hub");
        assertEquals("/hub", preprocessEvent.getMessage());
        assertFalse(preprocessEvent.isCancelled());
        preprocessEvent.setCancelled(true);
        assertTrue(preprocessEvent.isCancelled());

        ConsoleCommandSender console = new ConsoleCommandSender();
        ServerCommandEvent serverCmd = new ServerCommandEvent(console, "say Hello VESSEL");
        assertEquals(console, serverCmd.getSender());
        assertEquals("say Hello VESSEL", serverCmd.getCommand());
        serverCmd.setCommand("say Sovereign Online");
        assertEquals("say Sovereign Online", serverCmd.getCommand());

        VesselVirtualInventorySnapshot vusaGuiSnapshot = new VesselVirtualInventorySnapshot(
                hopperGui.getType().name(),
                hopperGui.getTitle(),
                hopperGui.getSize(),
                Map.of(0, "minecraft:diamond", 2, "minecraft:emerald"));
        assertEquals("HOPPER", vusaGuiSnapshot.inventoryTypeId());
        assertEquals("Custom Hopper", vusaGuiSnapshot.title());
        assertEquals(5, vusaGuiSnapshot.size());
        assertEquals(2, vusaGuiSnapshot.occupiedSlotCount());
        assertTrue(vusaGuiSnapshot.isSlotOccupied(0));
        assertTrue(vusaGuiSnapshot.isSlotOccupied(2));
        assertFalse(vusaGuiSnapshot.isSlotOccupied(1));
    }
}
