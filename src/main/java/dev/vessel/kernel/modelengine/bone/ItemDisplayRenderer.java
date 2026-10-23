package dev.vessel.kernel.modelengine.bone;

import dev.vessel.kernel.display.model.VirtualDisplayEntityBridge;

import java.util.Objects;

/**
 * Modern (1.19.4+) Minecraft Display Entity renderer simulator ({@code Display.ItemDisplay}).
 * Binds custom animated bones to ItemDisplay entities using CustomModelData item stacks,
 * 3D affine transformations (translation, quaternion rotation, scaling), and client GPU interpolation.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ItemDisplayRenderer {

    public static final String BILLBOARD_FIXED = "fixed";
    public static final String BILLBOARD_VERTICAL = "vertical";
    public static final String BILLBOARD_HORIZONTAL = "horizontal";
    public static final String BILLBOARD_CENTER = "center";

    private final BoneNode boneNode;
    private final VirtualDisplayEntityBridge displayBridge;

    /**
     * Constructs an ItemDisplayRenderer for a given bone and display bridge.
     *
     * @param boneNode      bone node definition
     * @param displayBridge virtual display entity bridge
     */
    public ItemDisplayRenderer(BoneNode boneNode, VirtualDisplayEntityBridge displayBridge) {
        this.boneNode = Objects.requireNonNull(boneNode, "boneNode cannot be null");
        this.displayBridge = Objects.requireNonNull(displayBridge, "displayBridge cannot be null");

        // Sync initial model data
        this.displayBridge.setCustomModelData(boneNode.getCustomModelData());
        this.displayBridge.setBillboardConstraint(BILLBOARD_FIXED);
        this.displayBridge.setInterpolationDurationTicks(3);
    }

    /**
     * Applies a computed bone transform to the display entity bridge.
     *
     * @param transform computed global bone transform
     */
    public void applyTransform(ComputedBoneTransform transform) {
        Objects.requireNonNull(transform, "transform cannot be null");

        float[] euler = transform.rotation().toEulerAngles();

        displayBridge.getTransformState().setTranslation(
                transform.translationX(),
                transform.translationY(),
                transform.translationZ()
        );
        displayBridge.getTransformState().setRotation(euler[0], euler[1], euler[2]);
        displayBridge.getTransformState().setScale(
                transform.scaleX(),
                transform.scaleY(),
                transform.scaleZ()
        );
    }

    public BoneNode getBoneNode() {
        return boneNode;
    }

    public VirtualDisplayEntityBridge getDisplayBridge() {
        return displayBridge;
    }

    public int getCustomModelData() {
        return boneNode.getCustomModelData();
    }

    public String getItemMaterial() {
        return boneNode.getItemMaterial();
    }
}
