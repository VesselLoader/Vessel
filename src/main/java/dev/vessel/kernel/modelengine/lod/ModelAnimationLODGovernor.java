package dev.vessel.kernel.modelengine.lod;

import dev.vessel.kernel.optimization.entity.PlayerSpatialCoordinates;

import java.util.Collection;

/**
 * Governor evaluating player proximity to custom animated model entities and assigning
 * the optimal {@link ModelAnimationLODTier}.
 * Throttles skeletal bone animation and network packet transmissions according to euclidean distance:
 * &lt; 16m: 20 TPS, 16-32m: 5 TPS, &gt; 32m: 0 TPS (frozen), unobserved: 0 CPU.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ModelAnimationLODGovernor {

    public static final double NEAR_DISTANCE_SQ = 16.0 * 16.0; // 256.0
    public static final double MID_DISTANCE_SQ  = 32.0 * 32.0; // 1024.0

    /**
     * Constructs a ModelAnimationLODGovernor.
     */
    public ModelAnimationLODGovernor() {
    }

    /**
     * Resolves the LOD tier based on squared distance to the nearest active player.
     *
     * @param minDistanceSq minimum squared distance to any observing player
     * @return resolved LOD tier
     */
    public ModelAnimationLODTier resolveTier(double minDistanceSq) {
        if (minDistanceSq < NEAR_DISTANCE_SQ) {
            return ModelAnimationLODTier.NEAR;
        } else if (minDistanceSq < MID_DISTANCE_SQ) {
            return ModelAnimationLODTier.MID;
        } else {
            return ModelAnimationLODTier.FAR;
        }
    }

    /**
     * Evaluates the active LOD tier from model entity coordinates and active player positions.
     *
     * @param modelX  entity position X
     * @param modelY  entity position Y
     * @param modelZ  entity position Z
     * @param players active players collection
     * @return active LOD tier (DORMANT if no players present)
     */
    public ModelAnimationLODTier resolveTier(
            double modelX, double modelY, double modelZ,
            Collection<PlayerSpatialCoordinates> players
    ) {
        if (players == null || players.isEmpty()) {
            return ModelAnimationLODTier.DORMANT;
        }

        double minDistanceSq = Double.MAX_VALUE;
        for (PlayerSpatialCoordinates player : players) {
            double dx = modelX - player.x();
            double dy = modelY - player.y();
            double dz = modelZ - player.z();
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq < minDistanceSq) {
                minDistanceSq = distSq;
                if (minDistanceSq < NEAR_DISTANCE_SQ) {
                    return ModelAnimationLODTier.NEAR;
                }
            }
        }

        return resolveTier(minDistanceSq);
    }

    /**
     * Determines whether bone animation updates should be computed and broadcast on this tick.
     *
     * @param modelId     unique model entity ID
     * @param modelX      entity position X
     * @param modelY      entity position Y
     * @param modelZ      entity position Z
     * @param players     active players collection
     * @param currentTick server tick count
     * @return true if bone calculations and packet updates should run
     */
    public boolean shouldUpdateBones(
            int modelId,
            double modelX, double modelY, double modelZ,
            Collection<PlayerSpatialCoordinates> players,
            long currentTick
    ) {
        ModelAnimationLODTier tier = resolveTier(modelX, modelY, modelZ, players);
        return tier.shouldUpdateAnimation(currentTick, modelId);
    }
}
