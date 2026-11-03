package org.bukkit.event.player;

/**
 * Canonical Bukkit {@code TeleportCause} enumeration representing the reason a
 * {@link PlayerTeleportEvent} was triggered.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public enum TeleportCause {
    ENDER_PEARL,
    COMMAND,
    PLUGIN,
    NETHER_PORTAL,
    END_PORTAL,
    SPECTATE,
    CHORUS_FRUIT,
    UNKNOWN
}
