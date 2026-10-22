package dev.vessel.kernel.player;

import dev.vessel.kernel.actor.RegionCoordinateHash;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable spatial coordinate snapshot for a player or entity.
 * <p>
 * Pre-computes the owning {@link RegionCoordinateHash} with bitshift arithmetic
 * to eliminate repeated spatial partitioning overhead in player movement ticks.
 * </p>
 *
 * @param playerId unique entity identifier
 * @param x world X position
 * @param y world Y position
 * @param z world Z position
 * @param yaw horizontal rotation in degrees
 * @param pitch vertical rotation in degrees
 * @param regionHash pre-computed 16x16 chunk region spatial hash
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-6)
 */
public record PlayerSpatialPosition(
        UUID playerId,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        RegionCoordinateHash regionHash
) {

    public PlayerSpatialPosition {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(regionHash, "regionHash cannot be null");
    }

    /**
     * Factory constructor computing spatial region hash from continuous coordinates.
     *
     * @param playerId player UUID
     * @param x world X
     * @param y world Y
     * @param z world Z
     * @param yaw yaw
     * @param pitch pitch
     * @return new PlayerSpatialPosition record
     */
    public static PlayerSpatialPosition of(UUID playerId, double x, double y, double z, float yaw, float pitch) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        RegionCoordinateHash hash = RegionCoordinateHash.fromBlock(blockX, blockZ);
        return new PlayerSpatialPosition(playerId, x, y, z, yaw, pitch, hash);
    }
}
