package net.minecraft.server.level;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Server-side Minecraft level representation ({@code net.minecraft.server.level.ServerLevel}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class ServerLevel extends Level {

    private final Map<UUID, ServerPlayer> players = new ConcurrentHashMap<>();

    /**
     * Constructs a new {@code ServerLevel} for the given dimension identifier.
     *
     * @param dimension dimension resource location
     */
    public ServerLevel(ResourceLocation dimension) {
        super(dimension);
    }

    public ServerPlayer getPlayerByUUID(UUID uuid) {
        return uuid != null ? players.get(uuid) : null;
    }

    public Collection<ServerPlayer> players() {
        return Collections.unmodifiableCollection(players.values());
    }

    public void addPlayer(ServerPlayer player) {
        if (player != null) {
            players.put(player.getUUID(), player);
        }
    }

    public void removePlayer(UUID uuid) {
        if (uuid != null) {
            players.remove(uuid);
        }
    }
}
