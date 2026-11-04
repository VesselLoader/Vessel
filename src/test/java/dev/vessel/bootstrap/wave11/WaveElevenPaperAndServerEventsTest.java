package dev.vessel.bootstrap.wave11;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.VesselWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

/**
 * Integration test verifying Paper PlayerTradeEvent, Bukkit PluginEnableEvent, and
 * PluginDisableEvent.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveElevenPaperAndServerEventsTest {

    @Test
    void testPaperTradeAndServerPluginEvents() {
        World world = new VesselWorld("world");
        Player player = new Player("Shiroz", new Location(world, 0.0D, 64.0D, 0.0D));

        ItemStack offered = new ItemStack(Material.EMERALD, 5);
        ItemStack result = new ItemStack(Material.DIAMOND, 1);
        PlayerTradeEvent tradeEvent = new PlayerTradeEvent(player, offered, result);
        assertEquals(player, tradeEvent.getPlayer());
        assertEquals(offered, tradeEvent.getOffered());
        assertEquals(result, tradeEvent.getResult());
        assertFalse(tradeEvent.isCancelled());
        tradeEvent.setCancelled(true);
        assertTrue(tradeEvent.isCancelled());

        JavaPlugin dummyPlugin = new JavaPlugin();
        PluginEnableEvent enableEvent = new PluginEnableEvent(dummyPlugin);
        assertEquals(dummyPlugin, enableEvent.getPlugin());

        PluginDisableEvent disableEvent = new PluginDisableEvent(dummyPlugin);
        assertEquals(dummyPlugin, disableEvent.getPlugin());
    }
}
