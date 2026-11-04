package net.minecraftforge.event.entity.player;

import java.util.UUID;

/**
 * Universal cross-era shim for Forge PlayerEvent$PlayerLoggedOutEvent.
 * Fired when a player logs out of the server.
 * Maintained under VESSEL Rule 17: Standalone single-purpose class.
 *
 * @version 1.0.0
 */
public class PlayerEvent$PlayerLoggedOutEvent extends PlayerEvent {

    public PlayerEvent$PlayerLoggedOutEvent() {
        super();
    }

    public PlayerEvent$PlayerLoggedOutEvent(Object player) {
        super(player);
    }

    public PlayerEvent$PlayerLoggedOutEvent(UUID playerUuid, String playerName) {
        super(playerUuid, playerName);
    }
}
