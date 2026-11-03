package dev.vessel.bootstrap.wave3;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Sample Bukkit gameplay {@link Listener} fixture verifying Wave 3 {@link PlayerJoinEvent},
 * {@link PlayerQuitEvent}, {@link BlockBreakEvent}, and {@link BlockPlaceEvent} dispatch.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class SampleBukkitGameplayListenerFixture implements Listener {

    private final List<String> gameplayLog = new ArrayList<>();

    public SampleBukkitGameplayListenerFixture() {
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage("Welcome to VESSEL, " + event.getPlayer().getName());
        gameplayLog.add("JOIN:" + event.getPlayer().getName());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        gameplayLog.add("QUIT:" + event.getPlayer().getName());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if ("minecraft:bedrock".equals(event.getBlock().getType())) {
            event.setCancelled(true);
            gameplayLog.add("BREAK_CANCELLED");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        gameplayLog.add("PLACE:" + event.getBlockPlaced().getType());
    }

    public List<String> getGameplayLog() {
        return gameplayLog;
    }
}
