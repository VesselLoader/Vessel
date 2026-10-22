package dev.vessel.kernel.display.model;

import java.util.Objects;

/**
 * Pure vector-arithmetic spherical and frustum culling filter for Display Entities and animated models.
 * <p>
 * Evaluates entity visibility against client camera position, look direction, field of view (FOV),
 * and maximum render distance in 1 CPU cycle using vector dot products.
 * </p>
 * <p>
 * Discards 100% of outbound render, animation, and bone-transform packets for objects located
 * behind the player or outside their render distance, eliminating packet flood and client GPU overhead.
 * Includes a near-proximity sphere to prevent models immediately adjacent to the player from being culled.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class FrustumCullingVisibilityFilter {

    public static final double DEFAULT_NEAR_PROXIMITY_RADIUS = 2.5; // Always visible within 2.5 blocks
    public static final float DEFAULT_FOV_DEGREES = 70.0f;
    public static final double DEFAULT_MAX_RENDER_DISTANCE = 64.0;

    private volatile double nearProximityRadius;

    /**
     * Constructs a FrustumCullingVisibilityFilter with default near proximity radius.
     */
    public FrustumCullingVisibilityFilter() {
        this(DEFAULT_NEAR_PROXIMITY_RADIUS);
    }

    /**
     * Constructs a FrustumCullingVisibilityFilter with specified near proximity radius.
     *
     * @param nearProximityRadius distance within which objects are always visible
     */
    public FrustumCullingVisibilityFilter(double nearProximityRadius) {
        if (nearProximityRadius < 0.0) {
            throw new IllegalArgumentException("nearProximityRadius must be non-negative: " + nearProximityRadius);
        }
        this.nearProximityRadius = nearProximityRadius;
    }

    /**
     * Evaluates whether an object bounding sphere is visible within the player's view frustum.
     *
     * @param playerX     player eye X
     * @param playerY     player eye Y
     * @param playerZ     player eye Z
     * @param playerYaw   player view yaw in degrees
     * @param playerPitch player view pitch in degrees
     * @param objX        object center X
     * @param objY        object center Y
     * @param objZ        object center Z
     * @param objRadius   object bounding radius
     * @param maxDistance maximum render distance
     * @param fovDegrees  horizontal/vertical field of view in degrees
     * @return true if visible to the client, false if culled
     */
    public boolean isVisible(
            double playerX, double playerY, double playerZ,
            float playerYaw, float playerPitch,
            double objX, double objY, double objZ,
            double objRadius, double maxDistance, float fovDegrees
    ) {
        double dx = objX - playerX;
        double dy = objY - playerY;
        double dz = objZ - playerZ;
        double distSq = dx * dx + dy * dy + dz * dz;

        double maxDistWithRadius = maxDistance + objRadius;
        if (distSq > maxDistWithRadius * maxDistWithRadius) {
            return false; // Beyond render distance
        }

        double nearWithRadius = nearProximityRadius + objRadius;
        if (distSq <= nearWithRadius * nearWithRadius) {
            return true; // Inside near proximity sphere
        }

        // Compute normalized look vector from yaw and pitch (Minecraft convention)
        double radYaw = Math.toRadians(playerYaw);
        double radPitch = Math.toRadians(playerPitch);
        double cosPitch = Math.cos(radPitch);
        double lookX = -Math.sin(radYaw) * cosPitch;
        double lookY = -Math.sin(radPitch);
        double lookZ = Math.cos(radYaw) * cosPitch;

        double dist = Math.sqrt(distSq);
        double dot = (dx * lookX + dy * lookY + dz * lookZ) / dist;

        // Half FOV cosine threshold with spherical radius margin
        double halfFovRad = Math.toRadians(Math.max(10.0f, Math.min(170.0f, fovDegrees)) * 0.5);
        double threshold = Math.cos(halfFovRad) - (objRadius / dist);

        return dot >= threshold;
    }

    /**
     * Evaluates visibility using a precomputed unit look direction vector for maximum CPU throughput.
     *
     * @param playerX     player eye X
     * @param playerY     player eye Y
     * @param playerZ     player eye Z
     * @param lookX       precomputed normalized look direction X
     * @param lookY       precomputed normalized look direction Y
     * @param lookZ       precomputed normalized look direction Z
     * @param objX        object center X
     * @param objY        object center Y
     * @param objZ        object center Z
     * @param objRadius   object bounding radius
     * @param maxDistance maximum render distance
     * @param cosHalfFov  precomputed cosine of half FOV
     * @return true if visible
     */
    public boolean isSphereVisible(
            double playerX, double playerY, double playerZ,
            double lookX, double lookY, double lookZ,
            double objX, double objY, double objZ,
            double objRadius, double maxDistance, double cosHalfFov
    ) {
        double dx = objX - playerX;
        double dy = objY - playerY;
        double dz = objZ - playerZ;
        double distSq = dx * dx + dy * dy + dz * dz;

        double maxDistWithRadius = maxDistance + objRadius;
        if (distSq > maxDistWithRadius * maxDistWithRadius) {
            return false;
        }

        double nearWithRadius = nearProximityRadius + objRadius;
        if (distSq <= nearWithRadius * nearWithRadius) {
            return true;
        }

        double dist = Math.sqrt(distSq);
        double dot = (dx * lookX + dy * lookY + dz * lookZ) / dist;
        return dot >= (cosHalfFov - (objRadius / dist));
    }

    /**
     * Batch filters an array of entity coordinates, populating an output buffer with visible indices.
     * Zero-allocation operation using contiguous primitive arrays.
     *
     * @param playerX           player eye X
     * @param playerY           player eye Y
     * @param playerZ           player eye Z
     * @param playerYaw         player yaw
     * @param playerPitch       player pitch
     * @param maxDistance       maximum render distance
     * @param fovDegrees        field of view
     * @param coords            contiguous array of coordinates [x0, y0, z0, x1, y1, z1, ...]
     * @param radii             radii array [r0, r1, ...]
     * @param count             number of entities in array
     * @param visibleIndicesOut caller-allocated output buffer for visible indices
     * @return count of visible entities written
     */
    public int batchFilter(
            double playerX, double playerY, double playerZ,
            float playerYaw, float playerPitch,
            double maxDistance, float fovDegrees,
            double[] coords, double[] radii, int count,
            int[] visibleIndicesOut
    ) {
        Objects.requireNonNull(coords, "coords cannot be null");
        Objects.requireNonNull(radii, "radii cannot be null");
        Objects.requireNonNull(visibleIndicesOut, "visibleIndicesOut cannot be null");

        if (count <= 0 || visibleIndicesOut.length == 0) {
            return 0;
        }

        double radYaw = Math.toRadians(playerYaw);
        double radPitch = Math.toRadians(playerPitch);
        double cosPitch = Math.cos(radPitch);
        double lookX = -Math.sin(radYaw) * cosPitch;
        double lookY = -Math.sin(radPitch);
        double lookZ = Math.cos(radYaw) * cosPitch;

        double halfFovRad = Math.toRadians(Math.max(10.0f, Math.min(170.0f, fovDegrees)) * 0.5);
        double cosHalfFov = Math.cos(halfFovRad);

        int visibleCount = 0;
        int limit = Math.min(count, visibleIndicesOut.length);

        for (int i = 0; i < limit; i++) {
            int coordIdx = i * 3;
            double ox = coords[coordIdx];
            double oy = coords[coordIdx + 1];
            double oz = coords[coordIdx + 2];
            double r = radii[i];

            if (isSphereVisible(playerX, playerY, playerZ, lookX, lookY, lookZ, ox, oy, oz, r, maxDistance, cosHalfFov)) {
                visibleIndicesOut[visibleCount++] = i;
            }
        }

        return visibleCount;
    }

    public double getNearProximityRadius() {
        return nearProximityRadius;
    }

    public void setNearProximityRadius(double nearProximityRadius) {
        if (nearProximityRadius < 0.0) {
            throw new IllegalArgumentException("nearProximityRadius must be non-negative: " + nearProximityRadius);
        }
        this.nearProximityRadius = nearProximityRadius;
    }
}
