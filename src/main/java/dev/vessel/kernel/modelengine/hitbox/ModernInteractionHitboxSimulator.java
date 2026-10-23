package dev.vessel.kernel.modelengine.hitbox;

import java.util.Objects;

/**
 * Simulator for modern (1.19.4+) Minecraft Interaction entities ({@code net.minecraft.world.entity.Interaction}).
 * Provides native bounding box dimensions that are visible in client F3 + B debug mode,
 * handling width/height updates and attack/interaction events.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class ModernInteractionHitboxSimulator {

    private final DynamicHitbox hitbox;
    private boolean response = true;

    /**
     * Constructs a ModernInteractionHitboxSimulator for a dynamic hitbox.
     *
     * @param hitbox dynamic hitbox to simulate
     */
    public ModernInteractionHitboxSimulator(DynamicHitbox hitbox) {
        this.hitbox = Objects.requireNonNull(hitbox, "hitbox cannot be null");
        if (hitbox.getHitboxType() != HitboxType.INTERACTION) {
            throw new IllegalArgumentException("Hitbox must be of type INTERACTION: " + hitbox.getHitboxType());
        }
    }

    /**
     * Updates hitbox dimensions (width and height).
     *
     * @param width  new width
     * @param height new height
     */
    public void resize(float width, float height) {
        this.hitbox.setWidth(width);
        this.hitbox.setHeight(height);
    }

    /**
     * Tests whether a 3D point is within the interaction hitbox's AABB.
     *
     * @param px point X
     * @param py point Y
     * @param pz point Z
     * @return true if point intersects bounding box
     */
    public boolean containsPoint(double px, double py, double pz) {
        if (!hitbox.isActive()) {
            return false;
        }
        float[] bbox = hitbox.computeBoundingBox();
        return px >= bbox[0] && px <= bbox[3]
                && py >= bbox[1] && py <= bbox[4]
                && pz >= bbox[2] && pz <= bbox[5];
    }

    /**
     * Tests whether a ray (e.g. arrow or melee raycast) intersects the interaction hitbox AABB.
     *
     * @param rayOriginX ray origin X
     * @param rayOriginY ray origin Y
     * @param rayOriginZ ray origin Z
     * @param rayDirX    ray direction unit vector X
     * @param rayDirY    ray direction unit vector Y
     * @param rayDirZ    ray direction unit vector Z
     * @param maxDist    maximum ray length
     * @return true if ray intersects hitbox within maxDist
     */
    public boolean intersectsRay(
            double rayOriginX, double rayOriginY, double rayOriginZ,
            double rayDirX, double rayDirY, double rayDirZ,
            double maxDist
    ) {
        if (!hitbox.isActive()) {
            return false;
        }

        float[] bbox = hitbox.computeBoundingBox();

        double invDirX = 1.0 / (Math.abs(rayDirX) < 1e-9 ? (rayDirX < 0.0 ? -1e-9 : 1e-9) : rayDirX);
        double invDirY = 1.0 / (Math.abs(rayDirY) < 1e-9 ? (rayDirY < 0.0 ? -1e-9 : 1e-9) : rayDirY);
        double invDirZ = 1.0 / (Math.abs(rayDirZ) < 1e-9 ? (rayDirZ < 0.0 ? -1e-9 : 1e-9) : rayDirZ);

        double t1 = (bbox[0] - rayOriginX) * invDirX;
        double t2 = (bbox[3] - rayOriginX) * invDirX;
        double t3 = (bbox[1] - rayOriginY) * invDirY;
        double t4 = (bbox[4] - rayOriginY) * invDirY;
        double t5 = (bbox[2] - rayOriginZ) * invDirZ;
        double t6 = (bbox[5] - rayOriginZ) * invDirZ;

        double tmin = Math.max(Math.max(Math.min(t1, t2), Math.min(t3, t4)), Math.min(t5, t6));
        double tmax = Math.min(Math.min(Math.max(t1, t2), Math.max(t3, t4)), Math.max(t5, t6));

        if (tmax < 0 || tmin > tmax) {
            return false;
        }

        double hitDist = (tmin < 0.0) ? 0.0 : tmin;
        return hitDist <= maxDist;
    }

    public DynamicHitbox getHitbox() {
        return hitbox;
    }

    public boolean isResponse() {
        return response;
    }

    public void setResponse(boolean response) {
        this.response = response;
    }
}
