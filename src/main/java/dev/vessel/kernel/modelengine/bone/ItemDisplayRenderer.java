package dev.vessel.kernel.modelengine.bone;

import dev.vessel.kernel.display.model.VirtualDisplayEntityBridge;
import dev.vessel.kernel.modelengine.lod.DisplayInterpolationConfig;

import java.util.Objects;

/**
 * Modern (1.19.4+) Minecraft Display Entity renderer simulator ({@code Display.ItemDisplay}).
 * Binds custom animated bones to ItemDisplay entities using CustomModelData item stacks,
 * 3D affine transformations (translation, quaternion rotation, scaling), and client GPU interpolation.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
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
     * Constructs an ItemDisplayRenderer for a given bone and display bridge with default 3-tick interpolation.
     *
     * @param boneNode      bone node definition
     * @param displayBridge virtual display entity bridge
     */
    public ItemDisplayRenderer(BoneNode boneNode, VirtualDisplayEntityBridge displayBridge) {
        this(boneNode, displayBridge, new DisplayInterpolationConfig(3));
    }

    /**
     * Constructs an ItemDisplayRenderer with a specific display interpolation configuration.
     *
     * @param boneNode            bone node definition
     * @param displayBridge       virtual display entity bridge
     * @param interpolationConfig display interpolation configuration
     */
    public ItemDisplayRenderer(
            BoneNode boneNode,
            VirtualDisplayEntityBridge displayBridge,
            DisplayInterpolationConfig interpolationConfig
    ) {
        this.boneNode = Objects.requireNonNull(boneNode, "boneNode cannot be null");
        this.displayBridge = Objects.requireNonNull(displayBridge, "displayBridge cannot be null");
        Objects.requireNonNull(interpolationConfig, "interpolationConfig cannot be null");

        // Sync initial model data
        this.displayBridge.setCustomModelData(boneNode.getCustomModelData());
        this.displayBridge.setBillboardConstraint(BILLBOARD_FIXED);
        applyInterpolationConfig(interpolationConfig);
    }

    /**
     * Applies the given interpolation configuration to the underlying display entity bridge.
     *
     * @param config display interpolation configuration
     */
    public void applyInterpolationConfig(DisplayInterpolationConfig config) {
        Objects.requireNonNull(config, "config cannot be null");
        this.displayBridge.setInterpolationDurationTicks(config.getInterpolationDurationTicks());
        this.displayBridge.setTeleportDurationTicks(config.getTeleportDurationTicks());
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
