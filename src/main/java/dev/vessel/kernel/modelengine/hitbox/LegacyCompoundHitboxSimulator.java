package dev.vessel.kernel.modelengine.hitbox;

import java.util.Objects;

/**
 * Simulator for legacy (<1.19.4) multi-part, slime-based, or armorstand-based compound hitboxes.
 * Emulates vanilla slime scaling (width = size * 0.51, height = size * 0.51) and forwards
 * attacks to the root entity on servers running legacy Minecraft compatibility layers.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class LegacyCompoundHitboxSimulator {

    public static final float SLIME_SIZE_FACTOR = 0.51f;

    private final DynamicHitbox hitbox;
    private int slimeSize = 1;

    /**
     * Constructs a LegacyCompoundHitboxSimulator for a legacy hitbox.
     *
     * @param hitbox dynamic hitbox to simulate
     */
    public LegacyCompoundHitboxSimulator(DynamicHitbox hitbox) {
        this.hitbox = Objects.requireNonNull(hitbox, "hitbox cannot be null");
        if (hitbox.getHitboxType() == HitboxType.INTERACTION) {
            throw new IllegalArgumentException("Hitbox must be a legacy type, not INTERACTION");
        }
    }

    /**
     * Computes the nearest discrete slime size that accommodates the required width and height.
     *
     * @param targetWidth  target bounding width
     * @param targetHeight target bounding height
     * @return discrete slime size integer (&gt;= 1)
     */
    public int computeSlimeSize(float targetWidth, float targetHeight) {
        float maxDim = Math.max(targetWidth, targetHeight);
        int size = Math.round(maxDim / SLIME_SIZE_FACTOR);
        return Math.max(1, size);
    }

    /**
     * Updates dimensions according to discrete slime size.
     *
     * @param size slime size
     */
    public void applySlimeSize(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("size must be >= 1: " + size);
        }
        this.slimeSize = size;
        float dimension = size * SLIME_SIZE_FACTOR;
        this.hitbox.setWidth(dimension);
        this.hitbox.setHeight(dimension);
    }

    public DynamicHitbox getHitbox() {
        return hitbox;
    }

    public int getSlimeSize() {
        return slimeSize;
    }
}
