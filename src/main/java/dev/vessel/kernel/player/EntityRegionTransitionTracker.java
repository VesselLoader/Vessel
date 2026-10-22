package dev.vessel.kernel.player;

import dev.vessel.kernel.actor.RegionCoordinateHash;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance tracker for player and entity spatial positions and regional boundary crossings.
 * <p>
 * Detects transitions across 16x16 chunk region boundaries, enabling seamless, lock-free
 * ownership handoffs between {@link dev.vessel.kernel.actor.RegionalActorStrand} executors.
 * </p>
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-6)
 */
public class EntityRegionTransitionTracker {

    private final ConcurrentHashMap<UUID, PlayerSpatialPosition> positions = new ConcurrentHashMap<>();

    /**
     * Updates an entity's spatial coordinates and checks for region boundary transitions.
     *
     * @param entityId entity or player UUID
     * @param x world X
     * @param y world Y
     * @param z world Z
     * @param yaw yaw
     * @param pitch pitch
     * @return Optional containing the previous RegionCoordinateHash if a boundary transition occurred
     */
    public Optional<RegionCoordinateHash> updatePosition(
            UUID entityId,
            double x,
            double y,
            double z,
            float yaw,
            float pitch
    ) {
        Objects.requireNonNull(entityId, "entityId cannot be null");
        PlayerSpatialPosition newPos = PlayerSpatialPosition.of(entityId, x, y, z, yaw, pitch);
        PlayerSpatialPosition previous = positions.put(entityId, newPos);

        if (previous != null && !previous.regionHash().equals(newPos.regionHash())) {
            return Optional.of(previous.regionHash());
        }
        return Optional.empty();
    }

    /**
     * Retrieves current spatial position for the entity.
     *
     * @param entityId entity UUID
     * @return position record or null if not registered
     */
    public PlayerSpatialPosition getPosition(UUID entityId) {
        Objects.requireNonNull(entityId, "entityId cannot be null");
        return positions.get(entityId);
    }

    /**
     * Retrieves the owning region coordinate hash for the entity.
     *
     * @param entityId entity UUID
     * @return owning RegionCoordinateHash or null if not registered
     */
    public RegionCoordinateHash getRegion(UUID entityId) {
        PlayerSpatialPosition pos = getPosition(entityId);
        return pos != null ? pos.regionHash() : null;
    }

    /**
     * Removes an entity from tracking upon disconnect or despawn.
     *
     * @param entityId entity UUID
     */
    public void removeEntity(UUID entityId) {
        Objects.requireNonNull(entityId, "entityId cannot be null");
        positions.remove(entityId);
    }

    /**
     * Returns total number of active entities currently tracked.
     *
     * @return tracked entity count
     */
    public int activeEntityCount() {
        return positions.size();
    }

    public void clear() {
        positions.clear();
    }
}
