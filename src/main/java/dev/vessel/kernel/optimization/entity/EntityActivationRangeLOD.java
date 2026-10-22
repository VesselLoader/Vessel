package dev.vessel.kernel.optimization.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Geometric radial Level-Of-Detail (LOD) entity activation manager.
 * Tiers AI brain calculations and pathfinding according to euclidean proximity to active players:
 * 1/1 ticks (&lt; 16m), 1/4 ticks (16..32m), 1/16 ticks (32..64m), and complete dormancy (&gt; 64m),
 * reducing mob tick processing load by up to 75% on crowded servers.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class EntityActivationRangeLOD {

    private static final double ACTIVE_SQ = 16.0 * 16.0;   // 256.0
    private static final double NEAR_SQ   = 32.0 * 32.0;   // 1024.0
    private static final double FAR_SQ    = 64.0 * 64.0;   // 4096.0

    private final Map<Integer, PlayerSpatialCoordinates> playerPositions = new ConcurrentHashMap<>();
    private final List<EntityActivationRule> exemptionRules = new ArrayList<>();

    /**
     * Constructs an EntityActivationRangeLOD manager.
     */
    public EntityActivationRangeLOD() {
    }

    /**
     * Registers an exemption rule (e.g. boss mobs or vehicles).
     *
     * @param rule custom activation rule
     */
    public synchronized void registerRule(EntityActivationRule rule) {
        Objects.requireNonNull(rule, "rule cannot be null");
        exemptionRules.add(rule);
    }

    /**
     * Updates or registers the spatial position of an active player.
     *
     * @param playerId player entity ID
     * @param x coordinate X
     * @param y coordinate Y
     * @param z coordinate Z
     */
    public void updatePlayerPosition(int playerId, double x, double y, double z) {
        playerPositions.put(playerId, new PlayerSpatialCoordinates(x, y, z));
    }

    /**
     * Removes a player when they disconnect or transition dimensions.
     *
     * @param playerId player entity ID
     */
    public void removePlayer(int playerId) {
        playerPositions.remove(playerId);
    }

    /**
     * Clears all tracked player positions.
     */
    public void clearPlayers() {
        playerPositions.clear();
    }

    /**
     * Evaluates whether an entity should execute its AI brain tick on the current game tick.
     *
     * @param entityId entity identifier
     * @param entityType namespaced entity type (e.g. "minecraft:zombie")
     * @param x entity position X
     * @param y entity position Y
     * @param z entity position Z
     * @param currentTick current server game tick
     * @return true if entity should tick
     */
    public boolean shouldTickEntity(
            int entityId,
            String entityType,
            double x,
            double y,
            double z,
            long currentTick) {
        EntityActivationTier tier = resolveTier(entityType, x, y, z);
        return tier.shouldTick(currentTick, entityId);
    }

    /**
     * Resolves the activation tier for an entity at the specified coordinates.
     *
     * @param entityType entity type string
     * @param x coordinate X
     * @param y coordinate Y
     * @param z coordinate Z
     * @return resolved activation tier
     */
    public EntityActivationTier resolveTier(String entityType, double x, double y, double z) {
        // Check exemption rules
        if (entityType != null && !exemptionRules.isEmpty()) {
            synchronized (this) {
                for (EntityActivationRule rule : exemptionRules) {
                    EntityActivationTier override = rule.resolveOverride(entityType);
                    if (override != null) {
                        return override;
                    }
                }
            }
        }

        if (playerPositions.isEmpty()) {
            return EntityActivationTier.DORMANT;
        }

        double minDistanceSq = Double.MAX_VALUE;
        for (PlayerSpatialCoordinates pos : playerPositions.values()) {
            double dx = x - pos.x();
            double dy = y - pos.y();
            double dz = z - pos.z();
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq < minDistanceSq) {
                minDistanceSq = distSq;
                if (minDistanceSq < ACTIVE_SQ) {
                    return EntityActivationTier.ACTIVE;
                }
            }
        }

        if (minDistanceSq < NEAR_SQ) {
            return EntityActivationTier.NEAR;
        } else if (minDistanceSq < FAR_SQ) {
            return EntityActivationTier.FAR;
        } else {
            return EntityActivationTier.DORMANT;
        }
    }
}
