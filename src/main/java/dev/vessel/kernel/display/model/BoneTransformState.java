package dev.vessel.kernel.display.model;

import java.util.Objects;

/**
 * Mutable transformation state of a model bone or sub-part.
 * Tracks translation, rotation, and scaling with fine-grained dirty bitmasks
 * to eliminate redundant network synchronization packets during animation ticks.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class BoneTransformState {

    public static final int DIRTY_TRANSLATION = 1 << 0;
    public static final int DIRTY_ROTATION    = 1 << 1;
    public static final int DIRTY_SCALE       = 1 << 2;

    private final String boneName;

    // Translation offsets
    private float translationX;
    private float translationY;
    private float translationZ;

    // Rotation (pitch, yaw, roll in radians)
    private float pitch;
    private float yaw;
    private float roll;

    // Scale factors
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private float scaleZ = 1.0f;

    // Bitmask tracking changes
    private int dirtyMask = 0;

    /**
     * Constructs a BoneTransformState with default identity transforms.
     *
     * @param boneName identifier of the bone
     */
    public BoneTransformState(String boneName) {
        this.boneName = Objects.requireNonNull(boneName, "boneName cannot be null");
    }

    /**
     * Updates translation coordinates and marks translation dirty if values changed.
     */
    public void setTranslation(float x, float y, float z) {
        if (this.translationX != x || this.translationY != y || this.translationZ != z) {
            this.translationX = x;
            this.translationY = y;
            this.translationZ = z;
            this.dirtyMask |= DIRTY_TRANSLATION;
        }
    }

    /**
     * Updates rotation angles (radians) and marks rotation dirty if values changed.
     */
    public void setRotation(float pitch, float yaw, float roll) {
        if (this.pitch != pitch || this.yaw != yaw || this.roll != roll) {
            this.pitch = pitch;
            this.yaw = yaw;
            this.roll = roll;
            this.dirtyMask |= DIRTY_ROTATION;
        }
    }

    /**
     * Updates scale factors and marks scale dirty if values changed.
     */
    public void setScale(float sx, float sy, float sz) {
        if (this.scaleX != sx || this.scaleY != sy || this.scaleZ != sz) {
            this.scaleX = sx;
            this.scaleY = sy;
            this.scaleZ = sz;
            this.dirtyMask |= DIRTY_SCALE;
        }
    }

    /**
     * Returns true if any transformation property has been modified.
     *
     * @return dirty status
     */
    public boolean isDirty() {
        return dirtyMask != 0;
    }

    /**
     * Checks if a specific transformation component is dirty.
     *
     * @param flag mask flag (DIRTY_TRANSLATION, DIRTY_ROTATION, DIRTY_SCALE)
     * @return true if specified flag is set
     */
    public boolean hasDirtyFlag(int flag) {
        return (dirtyMask & flag) != 0;
    }

    /**
     * Clears all dirty flags. Call after encoding state change packet.
     */
    public void clearDirty() {
        this.dirtyMask = 0;
    }

    public String getBoneName() {
        return boneName;
    }

    public float getTranslationX() {
        return translationX;
    }

    public float getTranslationY() {
        return translationY;
    }

    public float getTranslationZ() {
        return translationZ;
    }

    public float getPitch() {
        return pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public float getRoll() {
        return roll;
    }

    public float getScaleX() {
        return scaleX;
    }

    public float getScaleY() {
        return scaleY;
    }

    public float getScaleZ() {
        return scaleZ;
    }

    public int getDirtyMask() {
        return dirtyMask;
    }
}
