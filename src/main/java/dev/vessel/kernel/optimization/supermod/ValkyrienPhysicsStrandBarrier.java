package dev.vessel.kernel.optimization.supermod;

import dev.vessel.kernel.actor.RegionalActorStrand;
import dev.vessel.kernel.actor.primitive.RegionStrandRouter;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Synchronizes Valkyrien Skies 2 / Eureka physics ship frames with the owning Folia
 * {@link RegionalActorStrand} via {@link RegionStrandRouter}.
 *
 * @version 1.0.0
 */
public final class ValkyrienPhysicsStrandBarrier {

    private final Map<Long, Long> shipRegionKeys = new ConcurrentHashMap<>();

    /**
     * Synchronizes a physics ship's world coordinates with its owning regional actor strand.
     *
     * @param shipId the unique physics ship identifier
     * @param shipX  the ship center world X coordinate
     * @param shipY  the ship center world Y coordinate
     * @param shipZ  the ship center world Z coordinate
     * @param router the spatial region strand router
     * @return {@code true} if a valid owning regional actor strand was resolved and synchronized
     */
    public boolean synchronizeShipPhysicsFrame(
            long shipId,
            double shipX,
            double shipY,
            double shipZ,
            RegionStrandRouter router) {
        Objects.requireNonNull(router, "router must not be null");
        if (!Double.isFinite(shipX) || !Double.isFinite(shipY) || !Double.isFinite(shipZ)) {
            return false;
        }
        int blockX = (int) Math.floor(shipX);
        int blockZ = (int) Math.floor(shipZ);
        RegionalActorStrand strand = router.routeByBlock(blockX, blockZ);
        if (strand == null) {
            return false;
        }
        shipRegionKeys.put(shipId, strand.getRegionHash().toPackedKey());
        return true;
    }
}
