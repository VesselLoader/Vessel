package org.bukkit.entity;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Canonical Bukkit {@code Entity} base handle representing a spatial world entity with a unique
 * identifier, {@link EntityType}, {@link Location}, custom name, and lifecycle validity state.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class Entity {

    private final UUID uniqueId;
    private final EntityType type;
    private volatile Location location;
    private volatile String customName;
    private volatile boolean valid = true;

    public Entity(UUID uniqueId, EntityType type, Location location) {
        this.uniqueId = Objects.requireNonNull(uniqueId, "uniqueId must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.location = Objects.requireNonNull(location, "location must not be null");
    }

    public final UUID getUniqueId() {
        return uniqueId;
    }

    public final EntityType getType() {
        return type;
    }

    public final Location getLocation() {
        return location;
    }

    public final World getWorld() {
        return location != null ? location.getWorld() : null;
    }

    public final boolean teleport(Location destination) {
        this.location = Objects.requireNonNull(destination, "destination must not be null");
        return true;
    }

    public final String getCustomName() {
        return customName;
    }

    public final void setCustomName(String name) {
        this.customName = name;
    }

    public final boolean isValid() {
        return valid;
    }

    public final void remove() {
        this.valid = false;
    }
}
