package net.minecraftforge.event.entity.player;

import java.util.UUID;

/**
 * Universal cross-era shim for Forge PlayerEvent$PlayerLoggedInEvent.
 * Fired when a player logs into the server.
 * Maintained under VESSEL Rule 17: Standalone single-purpose class.
 *
 * @version 1.0.0
 */
public class PlayerEvent$PlayerLoggedInEvent extends PlayerEvent {

    public PlayerEvent$PlayerLoggedInEvent() {
        super();
    }

    public PlayerEvent$PlayerLoggedInEvent(Object player) {
        super(player);
    }

    public PlayerEvent$PlayerLoggedInEvent(UUID playerUuid, String playerName) {
        super(playerUuid, playerName);
    }
}
