package dev.vessel.adapter.bridge;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridges custom modded dimension portal transitions (Twilight Forest, The Aether,
 * Deeper and Darker) with Bukkit {@code PlayerPortalEvent} / Folia asynchronous
 * region teleportation semantics.
 *
 * @version 1.0.0
 */
public final class CustomPortalTeleportationBridge {

    private final Map<UUID, String> lastCompletedDestinationDimension = new ConcurrentHashMap<>();

    /**
     * Creates a new custom portal teleportation bridge.
     */
    public CustomPortalTeleportationBridge() {
    }

    /**
     * Evaluates and executes a modded portal teleportation after consulting Bukkit plugin
     * cancellation status and validating finite destination coordinates.
     *
     * @param playerUuid        UUID of the teleporting player
     * @param sourceDimension   origin dimension namespaced key
     * @param targetDimension   destination dimension namespaced key
     * @param destX             destination X coordinate
     * @param destY             destination Y coordinate
     * @param destZ             destination Z coordinate
     * @param cancelledByPlugin whether a Bukkit/Paper listener cancelled the portal event
     * @return {@code true} if the portal teleportation proceeded, {@code false} if aborted
     */
    public boolean fireSafeModPortalTeleport(
            UUID playerUuid,
            String sourceDimension,
            String targetDimension,
            double destX,
            double destY,
            double destZ,
            boolean cancelledByPlugin) {
        Objects.requireNonNull(playerUuid, "playerUuid must not be null");
        Objects.requireNonNull(sourceDimension, "sourceDimension must not be null");
        Objects.requireNonNull(targetDimension, "targetDimension must not be null");
        if (cancelledByPlugin) {
            return false;
        }
        if (!Double.isFinite(destX) || !Double.isFinite(destY) || !Double.isFinite(destZ)) {
            return false;
        }
        lastCompletedDestinationDimension.put(playerUuid, targetDimension.trim());
        return true;
    }
}
