package dev.vessel.bootstrap.wave5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Wave 5 verifying Bukkit {@link Permission}, {@link PermissionDefault},
 * {@link PotionEffect}, {@link PotionEffectType}, {@link Scoreboard}, {@link Objective},
 * {@link Score}, {@link DisplaySlot}, and {@link BossBar}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveFiveBukkitPermissibleScoreboardAndBossBarTest {

    @Test
    void testBukkitPermissionsPotionEffectsScoreboardAndBossBar() {
        World world = new World("world");
        Location location = new Location(world, 0.0, 64.0, 0.0);
        Player player = new Player("Shiroz", location);

        Permission adminPerm = new Permission("vessel.admin", "Admin node", PermissionDefault.OP);
        assertEquals("vessel.admin", adminPerm.getName());
        assertEquals("Admin node", adminPerm.getDescription());
        assertEquals(PermissionDefault.OP, adminPerm.getDefault());

        player.setOp(false);
        assertFalse(player.hasPermission(adminPerm));

        player.setPermission("vessel.admin", true);
        assertTrue(player.isPermissionSet("vessel.admin"));
        assertTrue(player.hasPermission(adminPerm));
        assertTrue(player.hasPermission("vessel.admin"));

        player.setPermission("vessel.restricted", false);
        assertFalse(player.hasPermission("vessel.restricted"));
        player.unsetPermission("vessel.restricted");
        assertFalse(player.isPermissionSet("vessel.restricted"));

        PotionEffect speedEffect = new PotionEffect(PotionEffectType.SPEED, 600, 1);
        assertTrue(player.addPotionEffect(speedEffect));
        assertTrue(player.hasPotionEffect(PotionEffectType.SPEED));
        PotionEffect activeSpeed = player.getPotionEffect(PotionEffectType.SPEED);
        assertNotNull(activeSpeed);
        assertEquals(600, activeSpeed.getDuration());
        assertEquals(1, activeSpeed.getAmplifier());
        assertEquals(1, player.getActivePotionEffects().size());

        player.removePotionEffect(PotionEffectType.SPEED);
        assertFalse(player.hasPotionEffect(PotionEffectType.SPEED));

        Scoreboard scoreboard = new Scoreboard();
        Objective objective = scoreboard.registerNewObjective("stats", "dummy", "Server Stats");
        assertEquals("stats", objective.getName());
        assertEquals("dummy", objective.getCriteria());
        assertEquals("Server Stats", objective.getDisplayName());
        assertTrue(objective.isRegistered());

        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        assertSame(objective, scoreboard.getObjective(DisplaySlot.SIDEBAR));

        Score shirozScore = objective.getScore("Shiroz");
        assertFalse(shirozScore.isScoreSet());
        shirozScore.setScore(42);
        assertTrue(shirozScore.isScoreSet());
        assertEquals(42, objective.getScore("Shiroz").getScore());

        BossBar bossBar = new BossBar("Sovereign Dragon", BarColor.PURPLE, BarStyle.SEGMENTED_10);
        assertEquals("Sovereign Dragon", bossBar.getTitle());
        assertEquals(BarColor.PURPLE, bossBar.getColor());
        assertEquals(BarStyle.SEGMENTED_10, bossBar.getStyle());
        bossBar.setProgress(0.75D);
        assertEquals(0.75D, bossBar.getProgress());

        bossBar.addPlayer(player);
        assertEquals(1, bossBar.getPlayers().size());
        assertSame(player, bossBar.getPlayers().get(0));
    }
}
