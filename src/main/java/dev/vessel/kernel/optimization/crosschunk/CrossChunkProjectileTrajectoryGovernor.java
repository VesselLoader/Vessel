package dev.vessel.kernel.optimization.crosschunk;

import dev.vessel.kernel.actor.RegionCoordinateHash;
import dev.vessel.kernel.actor.RegionalActorStrand;
import dev.vessel.kernel.actor.primitive.RegionStrandRouter;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Governs high-velocity modded projectiles (TACZ bullets, Point Blank firearms,
 * Create Big Cannons artillery shells, ICBM missiles, Ars Nouveau spell projectiles)
 * crossing chunk boundaries, preventing synchronous chunk loads on unloaded intermediate
 * chunks while supporting virtual ballistic RAM simulation and Folia region handoff.
 *
 * @version 1.0.0
 */
public final class CrossChunkProjectileTrajectoryGovernor {

    private final Map<Long, Long> virtualBallisticProjectiles = new ConcurrentHashMap<>();

    /**
     * Creates a new cross-chunk projectile trajectory governor.
     */
    public CrossChunkProjectileTrajectoryGovernor() {
    }

    /**
     * Determines whether a projectile should be allowed to physically step into
     * {@code (targetChunkX, targetChunkZ)}. Standard bullets are culled when hitting
     * an unloaded chunk boundary, whereas long-range artillery shells transition into
     * virtual RAM ballistic tracking without loading intermediate chunks.
     *
     * @param projectileId     unique projectile entity identifier
     * @param targetChunkX     destination chunk X coordinate
     * @param targetChunkZ     destination chunk Z coordinate
     * @param isChunkLoaded    whether the destination chunk is currently loaded in RAM
     * @param isArtilleryShell whether the projectile is a long-range artillery shell/missile
     * @return {@code true} if physical entity stepping into the chunk is permitted
     */
    public boolean shouldAllowProjectileStepIntoChunk(
            long projectileId,
            int targetChunkX,
            int targetChunkZ,
            boolean isChunkLoaded,
            boolean isArtilleryShell) {
        if (isChunkLoaded) {
            virtualBallisticProjectiles.remove(projectileId);
            return true;
        }
        if (isArtilleryShell) {
            long packedChunk = (((long) targetChunkX) << 32) | (targetChunkZ & 0xFFFFFFFFL);
            virtualBallisticProjectiles.put(projectileId, packedChunk);
        }
        return false;
    }

    /**
     * Computes the closed-form 3D ballistic position of a virtualized artillery shell
     * in RAM after {@code elapsedTicks} without ticking or loading intermediate chunks.
     *
     * @param startX       initial X coordinate
     * @param startY       initial Y coordinate
     * @param startZ       initial Z coordinate
     * @param velX         initial X velocity per tick
     * @param velY         initial Y velocity per tick
     * @param velZ         initial Z velocity per tick
     * @param gravity      downward gravity acceleration per tick squared
     * @param elapsedTicks ticks elapsed since launch
     * @return 3-element {@code double[]} array {@code [x, y, z]}
     */
    public double[] computeVirtualBallisticPosition(
            double startX,
            double startY,
            double startZ,
            double velX,
            double velY,
            double velZ,
            double gravity,
            int elapsedTicks) {
        int clampedTicks = Math.max(0, elapsedTicks);
        double t = clampedTicks;
        double x = startX + velX * t;
        double y = startY + velY * t - 0.5 * gravity * t * t;
        double z = startZ + velZ * t;
        return new double[]{x, y, z};
    }

    /**
     * Hands off a projectile entity across Folia $16 \times 16$ chunk regional strands
     * when its trajectory crosses a region boundary.
     *
     * @param projectileId unique projectile entity identifier
     * @param fromChunkX   source chunk X coordinate
     * @param fromChunkZ   source chunk Z coordinate
     * @param toChunkX     destination chunk X coordinate
     * @param toChunkZ     destination chunk Z coordinate
     * @param router       regional strand spatial router
     * @return {@code true} if the target region strand is registered or within the same region
     */
    public boolean handoffProjectileAcrossFoliaRegions(
            long projectileId,
            int fromChunkX,
            int fromChunkZ,
            int toChunkX,
            int toChunkZ,
            RegionStrandRouter router) {
        Objects.requireNonNull(router, "router must not be null");
        long fromKey = RegionCoordinateHash.fromChunk(fromChunkX, fromChunkZ).toPackedKey();
        long toKey = RegionCoordinateHash.fromChunk(toChunkX, toChunkZ).toPackedKey();
        if (fromKey == toKey) {
            return true;
        }
        RegionalActorStrand targetStrand = router.routeByChunk(toChunkX, toChunkZ);
        return targetStrand != null;
    }
}
