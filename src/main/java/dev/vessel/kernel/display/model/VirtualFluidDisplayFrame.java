package dev.vessel.kernel.display.model;

import java.util.Objects;

/**
 * Immutable transformation and visual state frame for a virtual BlockDisplay
 * rendering a simulated fluid surface.
 *
 * @param displayEntityId synthetic entity ID of the BlockDisplay
 * @param translationX horizontal X translation offset
 * @param translationY vertical Y surface level
 * @param translationZ horizontal Z translation offset
 * @param rollRad roll tilt in radians
 * @param pitchRad pitch tilt in radians
 * @param scaleX horizontal width scale
 * @param scaleY vertical height scale
 * @param scaleZ horizontal depth scale
 * @param blockStateId Minecraft block state (e.g. "minecraft:water[level=0]")
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record VirtualFluidDisplayFrame(
        int displayEntityId,
        float translationX,
        float translationY,
        float translationZ,
        float rollRad,
        float pitchRad,
        float scaleX,
        float scaleY,
        float scaleZ,
        String blockStateId
) {
    public VirtualFluidDisplayFrame {
        Objects.requireNonNull(blockStateId, "blockStateId cannot be null");
        if (scaleX <= 0.0f || scaleY <= 0.0f || scaleZ <= 0.0f) {
            throw new IllegalArgumentException("Scale dimensions must be strictly positive");
        }
    }
}
