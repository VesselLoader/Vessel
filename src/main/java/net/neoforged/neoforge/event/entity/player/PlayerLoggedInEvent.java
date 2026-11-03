package net.neoforged.neoforge.event.entity.player;

import java.util.Objects;
import java.util.UUID;

/**
 * NeoForge player login event ({@code net.neoforged.neoforge.event.entity.player.PlayerLoggedInEvent})
 * fired on {@code NeoForge.EVENT_BUS} when a player completes login.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class PlayerLoggedInEvent {

    private final UUID playerUuid;
    private final String playerName;

    /**
     * Constructs a new {@code PlayerLoggedInEvent} for the specified player UUID and username.
     *
     * @param playerUuid non-null player UUID
     * @param playerName non-null player username
     */
    public PlayerLoggedInEvent(UUID playerUuid, String playerName) {
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid must not be null");
        this.playerName = Objects.requireNonNull(playerName, "playerName must not be null").trim();
    }

    /**
     * Returns the UUID of the logged-in player.
     *
     * @return non-null player UUID
     */
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    /**
     * Returns the username of the logged-in player.
     *
     * @return non-null player name
     */
    public String getPlayerName() {
        return playerName;
    }
}
