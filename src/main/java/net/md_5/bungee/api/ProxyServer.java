package net.md_5.bungee.api;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * BungeeCord proxy server singleton facade ({@code net.md_5.bungee.api.ProxyServer}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class ProxyServer {

    private static volatile ProxyServer instance = new ProxyServer();
    private final Map<UUID, ProxiedPlayer> players = new ConcurrentHashMap<>();

    public ProxyServer() {
    }

    public static ProxyServer getInstance() {
        return instance;
    }

    public static void setInstance(ProxyServer newInstance) {
        if (newInstance != null) {
            instance = newInstance;
        }
    }

    public ProxiedPlayer getPlayer(UUID uuid) {
        return uuid != null ? players.get(uuid) : null;
    }

    public ProxiedPlayer getPlayer(String name) {
        if (name == null) {
            return null;
        }
        for (ProxiedPlayer p : players.values()) {
            if (name.equalsIgnoreCase(p.getName())) {
                return p;
            }
        }
        return null;
    }

    public Collection<ProxiedPlayer> getPlayers() {
        return Collections.unmodifiableCollection(players.values());
    }

    public void registerPlayer(ProxiedPlayer player) {
        if (player != null) {
            players.put(player.getUniqueId(), player);
        }
    }

    public void unregisterPlayer(UUID uuid) {
        if (uuid != null) {
            players.remove(uuid);
        }
    }
}
