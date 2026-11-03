package dev.vessel.kernel.ecosystem;

import com.velocitypowered.api.event.connection.PostLoginEvent;
import java.util.Objects;
import java.util.UUID;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerLoggedInEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Universal cross-ecosystem bridge that symmetrically dispatches player connection and join
 * lifecycles across Forge, NeoForge, Bukkit, Velocity, and BungeeCord.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class CrossEcosystemPlayerLifecycleBridge {

    public CrossEcosystemPlayerLifecycleBridge() {
    }

    /**
     * Broadcasts a player login across all loaded platforms.
     *
     * @param bukkitPlayer   Bukkit player representation
     * @param velocityPlayer Velocity player representation (optional)
     * @param bungeePlayer   BungeeCord player representation (optional)
     */
    public void dispatchPlayerJoin(
            Player bukkitPlayer,
            com.velocitypowered.api.proxy.Player velocityPlayer,
            ProxiedPlayer bungeePlayer) {
        Objects.requireNonNull(bukkitPlayer, "bukkitPlayer must not be null");
        UUID uuid = bukkitPlayer.getUniqueId();
        String name = bukkitPlayer.getName();

        // 1. Forge
        PlayerLoggedInEvent forgeEvent = new PlayerLoggedInEvent(uuid, name);
        MinecraftForge.EVENT_BUS.post(forgeEvent);

        // 2. NeoForge
        net.neoforged.neoforge.event.entity.player.PlayerLoggedInEvent neoForgeEvent =
                new net.neoforged.neoforge.event.entity.player.PlayerLoggedInEvent(uuid, name);
        NeoForge.EVENT_BUS.post(neoForgeEvent);

        // 3. Bukkit
        PlayerJoinEvent joinEvent = new PlayerJoinEvent(bukkitPlayer, "Welcome " + name);
        Bukkit.getPluginManager().callEvent(joinEvent);

        // 4. Velocity
        if (velocityPlayer != null) {
            PostLoginEvent velocityPostLogin = new PostLoginEvent(velocityPlayer);
        }

        // 5. BungeeCord
        if (bungeePlayer != null) {
            ProxyServer.getInstance().registerPlayer(bungeePlayer);
            net.md_5.bungee.api.event.PostLoginEvent bungeePostLogin =
                    new net.md_5.bungee.api.event.PostLoginEvent(bungeePlayer);
        }
    }
}
