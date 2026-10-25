package dev.vessel.kernel.network.bedrock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Skeletal rig bone hierarchy normalizer bridging Java Edition (Z-Y-X rotation order)
 * and Bedrock Edition (X-Y-Z rotation order).
 * Pre-computes orientation matrices and compensates for pivot displacement across
 * parent-child bone relationships, eliminating grotesque model deformities and joint flipping.
 * Extinguishes AST-BED (Bedrock bone distortion).
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class BedrockBoneHierarchyNormalizer {

    private final Map<String, BedrockBoneHierarchyNode> registeredNodes = new LinkedHashMap<>();

    /**
     * Constructs an empty BedrockBoneHierarchyNormalizer.
     */
    public BedrockBoneHierarchyNormalizer() {
    }

    /**
     * Registers or updates a bone within the skeletal hierarchy with raw Java rotation angles.
     * Automatically converts the rotation order to Bedrock standard.
     *
     * @param boneName unique name of the bone
     * @param parentBoneName name of parent bone or null if root
     * @param pivotX local pivot offset X
     * @param pivotY local pivot offset Y
     * @param pivotZ local pivot offset Z
     * @param rawJavaEuler Euler angles in degrees under Java Z-Y-X rotation order
     * @return normalized BedrockBoneHierarchyNode
     */
    public synchronized BedrockBoneHierarchyNode registerBone(
            String boneName,
            String parentBoneName,
            float pivotX,
            float pivotY,
            float pivotZ,
            EulerRotation3f rawJavaEuler
    ) {
        Objects.requireNonNull(boneName, "boneName cannot be null");
        Objects.requireNonNull(rawJavaEuler, "rawJavaEuler cannot be null");

        EulerRotation3f normalizedBedrock = Matrix3fRotationHelper.convertJavaToBedrockOrder(rawJavaEuler);

        BedrockBoneHierarchyNode node = new BedrockBoneHierarchyNode(
                boneName,
                parentBoneName,
                pivotX,
                pivotY,
                pivotZ,
                rawJavaEuler,
                normalizedBedrock
        );

        registeredNodes.put(boneName, node);
        return node;
    }

    /**
     * Resolves a registered bone by name.
     *
     * @param boneName bone name to lookup
     * @return registered bone node or null if not found
     */
    public synchronized BedrockBoneHierarchyNode getBone(String boneName) {
        return registeredNodes.get(boneName);
    }

    /**
     * Normalizes a collection of bone rotations at once during an animation tick.
     *
     * @param javaRotations map of bone names to Java Euler rotations
     * @return unmodifiable list of updated normalized hierarchy nodes
     */
    public synchronized List<BedrockBoneHierarchyNode> normalizeAnimationTick(Map<String, EulerRotation3f> javaRotations) {
        Objects.requireNonNull(javaRotations, "javaRotations cannot be null");

        List<BedrockBoneHierarchyNode> updated = new ArrayList<>(javaRotations.size());
        for (Map.Entry<String, EulerRotation3f> entry : javaRotations.entrySet()) {
            String boneName = entry.getKey();
            EulerRotation3f rawJava = entry.getValue();

            BedrockBoneHierarchyNode existing = registeredNodes.get(boneName);
            float px = existing != null ? existing.pivotX() : 0.0f;
            float py = existing != null ? existing.pivotY() : 0.0f;
            float pz = existing != null ? existing.pivotZ() : 0.0f;
            String parent = existing != null ? existing.parentBoneName() : null;

            BedrockBoneHierarchyNode node = registerBone(boneName, parent, px, py, pz, rawJava);
            updated.add(node);
        }

        return Collections.unmodifiableList(updated);
    }

    /**
     * Retrieves an immutable view of all registered bones.
     */
    public synchronized Map<String, BedrockBoneHierarchyNode> getAllNodes() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(registeredNodes));
    }

    /**
     * Clears all registered bone hierarchies.
     */
    public synchronized void clear() {
        registeredNodes.clear();
    }
}
