package dev.vessel.bootstrap.wave3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.VesselWorld;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Wave 3 verifying Bukkit {@link YamlConfiguration}, {@link World},
 * {@link Location}, {@link Block}, {@link Player}, and gameplay events
 * ({@link PlayerJoinEvent}, {@link PlayerQuitEvent}, {@link BlockBreakEvent},
 * {@link BlockPlaceEvent}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveThreeBukkitConfigAndGameplayEventsTest {

    @Test
    void testBukkitYamlConfigurationAndGameplayEventsSurface() {
        String rawYaml = "server:\n  motd: VESSEL Sovereign\n  max-players: 200\n";
        YamlConfiguration yaml = YamlConfiguration.loadConfigurationFromString(rawYaml);
        assertEquals("VESSEL Sovereign", yaml.getString("server.motd"));
        assertEquals(200, yaml.getInt("server.max-players"));

        yaml.addDefault("server.view-distance", 12);
        assertEquals(12, yaml.getInt("server.view-distance"));

        String serialized = yaml.saveToString();
        assertNotNull(serialized);
        assertTrue(serialized.contains("VESSEL Sovereign"));
        assertTrue(serialized.contains("200"));

        World overworld = new VesselWorld("world");
        assertEquals("world", overworld.getName());
        assertEquals(-64, overworld.getMinHeight());
        assertEquals(320, overworld.getMaxHeight());

        Location spawnLoc = new Location(overworld, 10.5, 64.0, -20.2);
        assertEquals(10, spawnLoc.getBlockX());
        assertEquals(64, spawnLoc.getBlockY());
        assertEquals(-21, spawnLoc.getBlockZ());

        Block bedrockBlock = overworld.getBlockAt(0, -64, 0);
        bedrockBlock.setType("minecraft:bedrock");
        assertEquals("minecraft:bedrock", bedrockBlock.getType());

        Block oakPlanksBlock = overworld.getBlockAt(1, 65, 1);
        oakPlanksBlock.setType("minecraft:oak_planks");

        Player steve = new Player("Steve", spawnLoc);
        assertTrue(steve.isOnline());
        steve.sendMessage("Hello Steve");
        assertEquals(List.of("Hello Steve"), steve.getReceivedMessages());

        PluginManager pm = Bukkit.getPluginManager();
        JavaPlugin hostPlugin = new JavaPlugin();
        hostPlugin.init(
                new PluginDescriptionFile(
                        "WaveThreeGameplayPlugin",
                        "1.0.0",
                        JavaPlugin.class.getName(),
                        "1.21"),
                Bukkit.getServer());
        pm.enablePlugin(hostPlugin);

        SampleBukkitGameplayListenerFixture listener = new SampleBukkitGameplayListenerFixture();
        pm.registerEvents(listener, hostPlugin);

        PlayerJoinEvent joinEvent = new PlayerJoinEvent(steve, "Steve joined the game");
        pm.callEvent(joinEvent);
        assertEquals("Welcome to VESSEL, Steve", joinEvent.getJoinMessage());

        BlockBreakEvent breakEvent = new BlockBreakEvent(bedrockBlock, steve);
        assertFalse(breakEvent.isCancelled());
        pm.callEvent(breakEvent);
        assertTrue(breakEvent.isCancelled());

        BlockPlaceEvent placeEvent = new BlockPlaceEvent(oakPlanksBlock, steve);
        pm.callEvent(placeEvent);
        assertFalse(placeEvent.isCancelled());

        PlayerQuitEvent quitEvent = new PlayerQuitEvent(steve, "Steve left the game");
        pm.callEvent(quitEvent);

        assertEquals(
                List.of(
                        "JOIN:Steve",
                        "BREAK_CANCELLED",
                        "PLACE:minecraft:oak_planks",
                        "QUIT:Steve"),
                listener.getGameplayLog());
    }
}
