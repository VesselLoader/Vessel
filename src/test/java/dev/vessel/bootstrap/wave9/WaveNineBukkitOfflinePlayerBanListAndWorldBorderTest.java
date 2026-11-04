package dev.vessel.bootstrap.wave9;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.BanListType;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.Environment;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.VesselWorld;
import org.bukkit.WorldBorder;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Wave 9 verifying Bukkit {@link OfflinePlayer}, {@link BanList},
 * {@link BanListType}, {@link BanEntry}, {@link Bukkit} (`v1.0.4`), {@link Difficulty},
 * {@link Environment}, {@link WorldBorder}, {@link World} (`v1.0.3`), {@link WeatherChangeEvent},
 * {@link ThunderChangeEvent}, and {@link WorldLoadEvent}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveNineBukkitOfflinePlayerBanListAndWorldBorderTest {

    @Test
    void testBukkitOfflinePlayerBanListWhitelistWorldBorderAndWeatherEvents() {
        Bukkit.setWhitelist(true);
        assertTrue(Bukkit.hasWhitelist());

        OfflinePlayer offline = Bukkit.getOfflinePlayer("Shiroz");
        assertNotNull(offline);
        assertEquals("Shiroz", offline.getName());
        assertFalse(offline.isOnline());
        assertTrue(offline.hasPlayedBefore());
        offline.setWhitelisted(true);
        assertTrue(offline.isWhitelisted());
        offline.setOp(true);
        assertTrue(offline.isOp());

        BanList nameBans = Bukkit.getBanList(BanListType.NAME);
        assertNotNull(nameBans);
        assertEquals(BanListType.NAME, nameBans.getType());

        BanEntry entry = nameBans.addBan("Griefer", "X-Ray", "Console");
        assertNotNull(entry);
        assertEquals("Griefer", entry.getTarget());
        assertEquals("X-Ray", entry.getReason());
        assertEquals("Console", entry.getSource());
        assertTrue(nameBans.isBanned("Griefer"));
        assertNotNull(nameBans.getBanEntry("Griefer"));

        nameBans.pardon("Griefer");
        assertFalse(nameBans.isBanned("Griefer"));

        World world = new VesselWorld("world_the_end");
        world.setDifficulty(Difficulty.HARD);
        world.setEnvironment(Environment.THE_END);
        world.setStorm(true);
        world.setThundering(true);

        assertEquals(Difficulty.HARD, world.getDifficulty());
        assertEquals(3, world.getDifficulty().getValue());
        assertEquals(Environment.THE_END, world.getEnvironment());
        assertTrue(world.hasStorm());
        assertTrue(world.isThundering());

        WorldBorder border = world.getWorldBorder();
        assertNotNull(border);
        border.setCenter(0.0D, 0.0D);
        border.setSize(100.0D);
        border.setDamageAmount(0.5D);
        border.setWarningDistance(10);

        assertEquals(100.0D, border.getSize());
        assertEquals(0.5D, border.getDamageAmount());
        assertEquals(10, border.getWarningDistance());
        assertTrue(border.isInside(new Location(world, 25.0D, 64.0D, -25.0D)));
        assertFalse(border.isInside(new Location(world, 75.0D, 64.0D, 0.0D)));

        WeatherChangeEvent weatherEvent = new WeatherChangeEvent(world, true);
        assertEquals(world, weatherEvent.getWorld());
        assertTrue(weatherEvent.toWeatherState());
        assertFalse(weatherEvent.isCancelled());
        weatherEvent.setCancelled(true);
        assertTrue(weatherEvent.isCancelled());

        ThunderChangeEvent thunderEvent = new ThunderChangeEvent(world, true);
        assertEquals(world, thunderEvent.getWorld());
        assertTrue(thunderEvent.toThunderState());
        assertFalse(thunderEvent.isCancelled());
        thunderEvent.setCancelled(true);
        assertTrue(thunderEvent.isCancelled());

        WorldLoadEvent loadEvent = new WorldLoadEvent(world);
        assertEquals(world, loadEvent.getWorld());

        Bukkit.setWhitelist(false);
    }
}
