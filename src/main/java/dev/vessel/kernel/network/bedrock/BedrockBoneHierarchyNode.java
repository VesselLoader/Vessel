package dev.vessel.kernel.network.bedrock;

import java.util.Objects;

/**
 * Immutable hierarchy node representing a bone in a Bedrock skeletal rig.
 *
 * @param boneName identifier of this bone
 * @param parentBoneName identifier of parent bone, or null for root
 * @param pivotX local pivot offset X
 * @param pivotY local pivot offset Y
 * @param pivotZ local pivot offset Z
 * @param rawJavaRotation original Euler rotation from Java animation (Z-Y-X order)
 * @param normalizedBedrockRotation converted Euler rotation for Bedrock (X-Y-Z order)
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record BedrockBoneHierarchyNode(
        String boneName,
        String parentBoneName,
        float pivotX,
        float pivotY,
        float pivotZ,
        EulerRotation3f rawJavaRotation,
        EulerRotation3f normalizedBedrockRotation
) {
    public BedrockBoneHierarchyNode {
        Objects.requireNonNull(boneName, "boneName cannot be null");
        Objects.requireNonNull(rawJavaRotation, "rawJavaRotation cannot be null");
        Objects.requireNonNull(normalizedBedrockRotation, "normalizedBedrockRotation cannot be null");
    }
}
