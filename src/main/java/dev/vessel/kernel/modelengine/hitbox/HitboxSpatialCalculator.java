package dev.vessel.kernel.modelengine.hitbox;

import java.util.Objects;

/**
 * Calculates world-space spatial positioning and bounding boxes for dynamic hitboxes
 * as the driver mob traverses the world, turns yaw/pitch, or executes bone animations.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class HitboxSpatialCalculator {

    /**
     * Constructs a HitboxSpatialCalculator.
     */
    public HitboxSpatialCalculator() {
    }

    /**
     * Computes and updates the world coordinates of a dynamic hitbox attached to the root driver mob.
     *
     * @param hitbox  dynamic hitbox to update
     * @param rootX   driver entity X
     * @param rootY   driver entity Y
     * @param rootZ   driver entity Z
     * @param rootYaw driver entity yaw in degrees
     */
    public void updateRootHitboxPosition(
            DynamicHitbox hitbox,
            double rootX, double rootY, double rootZ,
            float rootYaw
    ) {
        Objects.requireNonNull(hitbox, "hitbox cannot be null");
        double ox = hitbox.getOffsetX();
        double oy = hitbox.getOffsetY();
        double oz = hitbox.getOffsetZ();

        double radYaw = Math.toRadians(rootYaw);
        double cos = Math.cos(radYaw);
        double sin = Math.sin(radYaw);

        double rotatedX = ox * cos - oz * sin;
        double rotatedZ = oz * cos + ox * sin;

        hitbox.setWorldPosition(rootX + rotatedX, rootY + oy, rootZ + rotatedZ);
    }

    /**
     * Computes and updates the world coordinates of a dynamic hitbox attached to an animated bone.
     *
     * @param hitbox           dynamic hitbox to update
     * @param rootX            driver entity X
     * @param rootY            driver entity Y
     * @param rootZ            driver entity Z
     * @param rootYaw          driver entity yaw in degrees
     * @param boneTranslationX bone global translation X
     * @param boneTranslationY bone global translation Y
     * @param boneTranslationZ bone global translation Z
     */
    public void updateBoneHitboxPosition(
            DynamicHitbox hitbox,
            double rootX, double rootY, double rootZ,
            float rootYaw,
            float boneTranslationX, float boneTranslationY, float boneTranslationZ
    ) {
        Objects.requireNonNull(hitbox, "hitbox cannot be null");
        double combinedOx = hitbox.getOffsetX() + boneTranslationX;
        double combinedOy = hitbox.getOffsetY() + boneTranslationY;
        double combinedOz = hitbox.getOffsetZ() + boneTranslationZ;

        double radYaw = Math.toRadians(rootYaw);
        double cos = Math.cos(radYaw);
        double sin = Math.sin(radYaw);

        double rotatedX = combinedOx * cos - combinedOz * sin;
        double rotatedZ = combinedOz * cos + combinedOx * sin;

        hitbox.setWorldPosition(rootX + rotatedX, rootY + combinedOy, rootZ + rotatedZ);
    }
}
