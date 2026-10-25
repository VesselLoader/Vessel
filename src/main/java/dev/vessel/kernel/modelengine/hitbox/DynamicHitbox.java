package dev.vessel.kernel.modelengine.hitbox;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Representation of a dynamic, localized hitbox attached to a Model Engine entity or bone.
 * Intercepts player melee, projectile, and interaction events, translating them to the root entity
 * with body-part specific damage multipliers.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class DynamicHitbox {

    private static final AtomicInteger HITBOX_ID_GENERATOR = new AtomicInteger(5_000_000);

    private final String hitboxId;
    private final int virtualEntityId;
    private final HitboxType hitboxType;
    private final String attachedBone;

    private double offsetX;
    private double offsetY;
    private double offsetZ;

    private float width;
    private float height;
    private float damageMultiplier = 1.0f;

    private double worldX;
    private double worldY;
    private double worldZ;

    private boolean active = true;

    /**
     * Constructs a DynamicHitbox with an auto-generated virtual entity ID.
     *
     * @param hitboxId     unique hitbox identifier (e.g. "head", "body", "left_wing")
     * @param hitboxType   hitbox simulation type
     * @param attachedBone name of bone this hitbox tracks, or null if anchored to root
     * @param width        hitbox width in blocks
     * @param height       hitbox height in blocks
     */
    public DynamicHitbox(String hitboxId, HitboxType hitboxType, String attachedBone, float width, float height) {
        this(hitboxId, HITBOX_ID_GENERATOR.incrementAndGet(), hitboxType, attachedBone, width, height);
    }

    /**
     * Constructs a DynamicHitbox with an explicit virtual entity ID.
     */
    public DynamicHitbox(String hitboxId, int virtualEntityId, HitboxType hitboxType, String attachedBone, float width, float height) {
        this.hitboxId = Objects.requireNonNull(hitboxId, "hitboxId cannot be null");
        this.virtualEntityId = virtualEntityId;
        this.hitboxType = Objects.requireNonNull(hitboxType, "hitboxType cannot be null");
        this.attachedBone = attachedBone;
        setWidth(width);
        setHeight(height);
    }

    public void setOffsets(double ox, double oy, double oz) {
        this.offsetX = ox;
        this.offsetY = oy;
        this.offsetZ = oz;
    }

    public void setWorldPosition(double wx, double wy, double wz) {
        this.worldX = wx;
        this.worldY = wy;
        this.worldZ = wz;
    }

    /**
     * Computes the bounding box extents [minX, minY, minZ, maxX, maxY, maxZ] in world space.
     *
     * @return 6-element float array of AABB coordinates
     */
    public float[] computeBoundingBox() {
        float halfW = width * 0.5f;
        return new float[]{
                (float) (worldX - halfW),
                (float) worldY,
                (float) (worldZ - halfW),
                (float) (worldX + halfW),
                (float) (worldY + height),
                (float) (worldZ + halfW)
        };
    }

    public String getHitboxId() {
        return hitboxId;
    }

    public int getVirtualEntityId() {
        return virtualEntityId;
    }

    public HitboxType getHitboxType() {
        return hitboxType;
    }

    public String getAttachedBone() {
        return attachedBone;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(double offsetX) {
        this.offsetX = offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(double offsetY) {
        this.offsetY = offsetY;
    }

    public double getOffsetZ() {
        return offsetZ;
    }

    public void setOffsetZ(double offsetZ) {
        this.offsetZ = offsetZ;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        if (width <= 0.0f) {
            throw new IllegalArgumentException("width must be positive: " + width);
        }
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        if (height <= 0.0f) {
            throw new IllegalArgumentException("height must be positive: " + height);
        }
        this.height = height;
    }

    public float getDamageMultiplier() {
        return damageMultiplier;
    }

    public void setDamageMultiplier(float damageMultiplier) {
        if (damageMultiplier < 0.0f) {
            throw new IllegalArgumentException("damageMultiplier cannot be negative: " + damageMultiplier);
        }
        this.damageMultiplier = damageMultiplier;
    }

    public double getWorldX() {
        return worldX;
    }

    public double getWorldY() {
        return worldY;
    }

    public double getWorldZ() {
        return worldZ;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
