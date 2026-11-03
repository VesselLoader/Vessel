package net.minecraftforge.network;

import java.util.Objects;
import java.util.UUID;

/**
 * Forge packet routing destination descriptor ({@code net.minecraftforge.network.PacketTarget}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class PacketTarget {

    private final String targetType;
    private final UUID targetPlayerUuid;

    /**
     * Constructs a new {@code PacketTarget} for the specified routing target type and optional player UUID.
     *
     * @param targetType       non-null target routing mode (e.g., {@code "PLAYER"}, {@code "ALL"}, {@code "SERVER"})
     * @param targetPlayerUuid target player UUID when routing to a specific player, or {@code null}
     */
    public PacketTarget(String targetType, UUID targetPlayerUuid) {
        this.targetType = Objects.requireNonNull(targetType, "targetType must not be null");
        this.targetPlayerUuid = targetPlayerUuid;
    }

    /**
     * Creates a packet target routing to a single connected player.
     *
     * @param playerUuid non-null target player UUID
     * @return player packet target
     */
    public static PacketTarget player(UUID playerUuid) {
        return new PacketTarget("PLAYER", Objects.requireNonNull(playerUuid, "playerUuid must not be null"));
    }

    /**
     * Creates a packet target broadcasting to all connected players.
     *
     * @return broadcast packet target
     */
    public static PacketTarget all() {
        return new PacketTarget("ALL", null);
    }

    /**
     * Creates a packet target routing to the dedicated/integrated server.
     *
     * @return server packet target
     */
    public static PacketTarget server() {
        return new PacketTarget("SERVER", null);
    }

    /**
     * Returns the routing target mode identifier.
     *
     * @return non-null target type
     */
    public String getTargetType() {
        return targetType;
    }

    /**
     * Returns the target player UUID if this target is player-specific, or {@code null} otherwise.
     *
     * @return target player UUID or {@code null}
     */
    public UUID getTargetPlayerUuid() {
        return targetPlayerUuid;
    }
}
