package dev.vessel.kernel.modelengine.bone;

import java.util.Objects;

/**
 * Definition of an individual bone in a Model Engine animated entity model hierarchy.
 * Tracks local binding transformations, parent linkage, and resourcepack CustomModelData.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class BoneNode {

    private final String boneName;
    private final String parentBoneName;

    private float translationX;
    private float translationY;
    private float translationZ;

    private float pitch;
    private float yaw;
    private float roll;

    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private float scaleZ = 1.0f;

    private int customModelData;
    private String itemMaterial = "minecraft:leather_horse_armor";

    /**
     * Constructs a BoneNode with default identity transforms.
     *
     * @param boneName       unique bone name
     * @param parentBoneName name of parent bone (null if root bone)
     */
    public BoneNode(String boneName, String parentBoneName) {
        this.boneName = Objects.requireNonNull(boneName, "boneName cannot be null");
        this.parentBoneName = parentBoneName;
    }

    public void setTranslation(float x, float y, float z) {
        this.translationX = x;
        this.translationY = y;
        this.translationZ = z;
    }

    public void setRotation(float pitch, float yaw, float roll) {
        this.pitch = pitch;
        this.yaw = yaw;
        this.roll = roll;
    }

    public void setScale(float sx, float sy, float sz) {
        this.scaleX = sx;
        this.scaleY = sy;
        this.scaleZ = sz;
    }

    public String getBoneName() {
        return boneName;
    }

    public String getParentBoneName() {
        return parentBoneName;
    }

    public boolean isRootBone() {
        return parentBoneName == null || parentBoneName.isEmpty();
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

    public int getCustomModelData() {
        return customModelData;
    }

    public void setCustomModelData(int customModelData) {
        this.customModelData = customModelData;
    }

    public String getItemMaterial() {
        return itemMaterial;
    }

    public void setItemMaterial(String itemMaterial) {
        this.itemMaterial = Objects.requireNonNull(itemMaterial, "itemMaterial cannot be null");
    }
}
