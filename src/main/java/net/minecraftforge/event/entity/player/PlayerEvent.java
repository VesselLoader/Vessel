package net.minecraftforge.event.entity.player;

import java.util.Objects;
import java.util.UUID;

/**
 * Base Forge player event ({@code net.minecraftforge.event.entity.player.PlayerEvent}) exposing the
 * player's UUID and username.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public class PlayerEvent {

    private final UUID playerUuid;
    private final String playerName;

    public PlayerEvent() {
        this(new UUID(0L, 0L), "Player");
    }

    public PlayerEvent(Object player) {
        this(new UUID(0L, 0L), player != null ? player.toString() : "Player");
    }

    /**
     * Constructs a new {@code PlayerEvent} for the specified player UUID and username.
     *
     * @param playerUuid non-null player UUID
     * @param playerName non-null player username
     */
    public PlayerEvent(UUID playerUuid, String playerName) {
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid must not be null");
        this.playerName = Objects.requireNonNull(playerName, "playerName must not be null").trim();
    }

    /**
     * Returns the UUID of the player associated with this event.
     *
     * @return non-null player UUID
     */
    public final UUID getPlayerUuid() {
        return playerUuid;
    }

    /**
     * Returns the username of the player associated with this event.
     *
     * @return non-null player name
     */
    public final String getPlayerName() {
        return playerName;
    }
}
