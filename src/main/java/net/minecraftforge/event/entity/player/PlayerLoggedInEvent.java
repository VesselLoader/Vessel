package net.minecraftforge.event.entity.player;

import java.util.UUID;

/**
 * Forge player login event ({@code net.minecraftforge.event.entity.player.PlayerLoggedInEvent})
 * fired on {@code MinecraftForge.EVENT_BUS} when a player completes login.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class PlayerLoggedInEvent extends PlayerEvent {

    /**
     * Constructs a new {@code PlayerLoggedInEvent} for the specified player UUID and username.
     *
     * @param playerUuid non-null player UUID
     * @param playerName non-null player username
     */
    public PlayerLoggedInEvent(UUID playerUuid, String playerName) {
        super(playerUuid, playerName);
    }
}
