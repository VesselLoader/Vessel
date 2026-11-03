package org.bukkit;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a persistent player profile record (potentially offline) known to the server
 * ({@code org.bukkit.OfflinePlayer}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class OfflinePlayer {

    private final UUID uniqueId;
    private final String name;
    private volatile boolean op;
    private volatile boolean whitelisted;
    private volatile boolean banned;
    private volatile boolean playedBefore = true;

    /**
     * Constructs a new {@code OfflinePlayer} record.
     *
     * @param uniqueId non-null player UUID
     * @param name     non-null player username
     */
    public OfflinePlayer(UUID uniqueId, String name) {
        this.uniqueId = Objects.requireNonNull(uniqueId, "uniqueId must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    public final UUID getUniqueId() {
        return uniqueId;
    }

    public final String getName() {
        return name;
    }

    public final boolean isOp() {
        return op;
    }

    public final void setOp(boolean value) {
        this.op = value;
    }

    public final boolean isWhitelisted() {
        return whitelisted;
    }

    public final void setWhitelisted(boolean value) {
        this.whitelisted = value;
    }

    public final boolean isBanned() {
        return banned;
    }

    public final void setBanned(boolean banned) {
        this.banned = banned;
    }

    public final boolean hasPlayedBefore() {
        return playedBefore;
    }

    public final boolean isOnline() {
        return false;
    }
}
