package dev.vessel.kernel.modelengine.bone;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Legacy (<1.19.4) Minecraft ArmorStand renderer simulator for Model Engine bone parts.
 * Uses small invisible marker ArmorStands with helmet slot CustomModelData and HeadPose
 * Euler angle rotations to render 3D models on legacy servers.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class LegacyArmorStandRenderer {

    private static final AtomicInteger STAND_ID_GENERATOR = new AtomicInteger(7_000_000);

    private final BoneNode boneNode;
    private final int armorStandEntityId;

    private boolean small = true;
    private boolean marker = true;
    private boolean invisible = true;

    private float headPosePitch;
    private float headPoseYaw;
    private float headPoseRoll;

    private float worldX;
    private float worldY;
    private float worldZ;

    /**
     * Constructs a LegacyArmorStandRenderer with an auto-generated entity ID.
     *
     * @param boneNode bone node to render
     */
    public LegacyArmorStandRenderer(BoneNode boneNode) {
        this(boneNode, STAND_ID_GENERATOR.incrementAndGet());
    }

    /**
     * Constructs a LegacyArmorStandRenderer with an explicit entity ID.
     */
    public LegacyArmorStandRenderer(BoneNode boneNode, int armorStandEntityId) {
        this.boneNode = Objects.requireNonNull(boneNode, "boneNode cannot be null");
        this.armorStandEntityId = armorStandEntityId;
    }

    /**
     * Applies a computed bone transform to the armorstand's HeadPose and position.
     *
     * @param transform computed global bone transform
     */
    public void applyTransform(ComputedBoneTransform transform) {
        Objects.requireNonNull(transform, "transform cannot be null");

        this.worldX = transform.translationX();
        this.worldY = transform.translationY();
        this.worldZ = transform.translationZ();

        float[] euler = transform.rotation().toEulerAngles();
        // HeadPose in degrees
        this.headPosePitch = (float) Math.toDegrees(euler[0]);
        this.headPoseYaw = (float) Math.toDegrees(euler[1]);
        this.headPoseRoll = (float) Math.toDegrees(euler[2]);
    }

    public BoneNode getBoneNode() {
        return boneNode;
    }

    public int getArmorStandEntityId() {
        return armorStandEntityId;
    }

    public boolean isSmall() {
        return small;
    }

    public void setSmall(boolean small) {
        this.small = small;
    }

    public boolean isMarker() {
        return marker;
    }

    public void setMarker(boolean marker) {
        this.marker = marker;
    }

    public boolean isInvisible() {
        return invisible;
    }

    public void setInvisible(boolean invisible) {
        this.invisible = invisible;
    }

    public float getHeadPosePitch() {
        return headPosePitch;
    }

    public float getHeadPoseYaw() {
        return headPoseYaw;
    }

    public float getHeadPoseRoll() {
        return headPoseRoll;
    }

    public float getWorldX() {
        return worldX;
    }

    public float getWorldY() {
        return worldY;
    }

    public float getWorldZ() {
        return worldZ;
    }

    public int getCustomModelData() {
        return boneNode.getCustomModelData();
    }
}
