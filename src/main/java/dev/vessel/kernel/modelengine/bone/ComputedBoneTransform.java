package dev.vessel.kernel.modelengine.bone;

import java.util.Objects;

/**
 * Immutable evaluated world transformation of a bone in the skeletal hierarchy.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param boneName     name of the evaluated bone
 * @param translationX world translation X
 * @param translationY world translation Y
 * @param translationZ world translation Z
 * @param rotation     world rotation quaternion
 * @param scaleX       world scale X
 * @param scaleY       world scale Y
 * @param scaleZ       world scale Z
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record ComputedBoneTransform(
        String boneName,
        float translationX,
        float translationY,
        float translationZ,
        QuaternionRotation rotation,
        float scaleX,
        float scaleY,
        float scaleZ
) {
    /**
     * Constructs a ComputedBoneTransform with validation.
     */
    public ComputedBoneTransform {
        Objects.requireNonNull(boneName, "boneName cannot be null");
        Objects.requireNonNull(rotation, "rotation cannot be null");
    }
}
