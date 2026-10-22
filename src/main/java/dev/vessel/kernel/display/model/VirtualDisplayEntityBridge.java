package dev.vessel.kernel.display.model;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * High-performance virtual bridge mapping custom animated model parts to
 * Minecraft vanilla Display Entities (Item Display / Block Display / Text Display).
 * Generates compact synthetic entity descriptors enabling vanilla Java and Bedrock clients
 * to render complex modded models without client-side mod installations.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class VirtualDisplayEntityBridge {

    private static final AtomicInteger ENTITY_ID_COUNTER = new AtomicInteger(1_000_000);

    private final int virtualEntityId;
    private final String partName;
    private final BoneTransformState transformState;
    private int interpolationDurationTicks = 3;
    private float viewRange = 64.0f;
    private boolean glowing = false;

    /**
     * Constructs a VirtualDisplayEntityBridge with a generated synthetic entity ID.
     *
     * @param partName unique model part name
     * @param transformState associated bone transformation state
     */
    public VirtualDisplayEntityBridge(String partName, BoneTransformState transformState) {
        this.virtualEntityId = ENTITY_ID_COUNTER.incrementAndGet();
        this.partName = Objects.requireNonNull(partName, "partName cannot be null");
        this.transformState = Objects.requireNonNull(transformState, "transformState cannot be null");
    }

    /**
     * Constructs a VirtualDisplayEntityBridge with an explicit synthetic entity ID.
     */
    public VirtualDisplayEntityBridge(int entityId, String partName, BoneTransformState transformState) {
        this.virtualEntityId = entityId;
        this.partName = Objects.requireNonNull(partName, "partName cannot be null");
        this.transformState = Objects.requireNonNull(transformState, "transformState cannot be null");
    }

    public int getVirtualEntityId() {
        return virtualEntityId;
    }

    public String getPartName() {
        return partName;
    }

    public BoneTransformState getTransformState() {
        return transformState;
    }

    public int getInterpolationDurationTicks() {
        return interpolationDurationTicks;
    }

    public void setInterpolationDurationTicks(int ticks) {
        if (ticks < 0) {
            throw new IllegalArgumentException("ticks cannot be negative");
        }
        this.interpolationDurationTicks = ticks;
    }

    public float getViewRange() {
        return viewRange;
    }

    public void setViewRange(float viewRange) {
        if (viewRange < 0.0f) {
            throw new IllegalArgumentException("viewRange cannot be negative");
        }
        this.viewRange = viewRange;
    }

    public boolean isGlowing() {
        return glowing;
    }

    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
    }
}
