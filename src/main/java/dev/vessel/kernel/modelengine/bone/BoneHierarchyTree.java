package dev.vessel.kernel.modelengine.bone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Manages the skeletal hierarchy tree of bones and calculates composite forward-kinematics
 * transformations from root bones down to leaf bones.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class BoneHierarchyTree {

    private final Map<String, BoneNode> bones = new LinkedHashMap<>();
    private final Map<String, List<String>> childrenMap = new LinkedHashMap<>();

    /**
     * Constructs an empty BoneHierarchyTree.
     */
    public BoneHierarchyTree() {
    }

    /**
     * Registers a bone node into the hierarchy tree.
     *
     * @param bone bone node to register
     */
    public synchronized void registerBone(BoneNode bone) {
        Objects.requireNonNull(bone, "bone cannot be null");
        bones.put(bone.getBoneName(), bone);

        String parent = bone.getParentBoneName();
        if (parent != null && !parent.isEmpty()) {
            List<String> children = childrenMap.get(parent);
            if (children == null) {
                children = new ArrayList<>();
                childrenMap.put(parent, children);
            }
            if (!children.contains(bone.getBoneName())) {
                children.add(bone.getBoneName());
            }
        }
    }

    /**
     * Evaluates all global/composite bone transforms by propagating local transformations
     * from roots down the hierarchy tree.
     *
     * @return map of bone name to computed global transformation
     */
    public synchronized Map<String, ComputedBoneTransform> computeGlobalTransforms() {
        Map<String, ComputedBoneTransform> results = new LinkedHashMap<>();

        // Process all root bones first
        for (BoneNode bone : bones.values()) {
            if (bone.isRootBone()) {
                evaluateSubtree(bone, null, results);
            }
        }

        return Collections.unmodifiableMap(results);
    }

    /**
     * Recursively evaluates global transformations for a bone and its children.
     */
    private void evaluateSubtree(
            BoneNode bone,
            ComputedBoneTransform parentTransform,
            Map<String, ComputedBoneTransform> results
    ) {
        QuaternionRotation localRot = QuaternionRotation.fromEuler(bone.getPitch(), bone.getYaw(), bone.getRoll());

        ComputedBoneTransform currentTransform;
        if (parentTransform == null) {
            currentTransform = new ComputedBoneTransform(
                    bone.getBoneName(),
                    bone.getTranslationX(),
                    bone.getTranslationY(),
                    bone.getTranslationZ(),
                    localRot,
                    bone.getScaleX(),
                    bone.getScaleY(),
                    bone.getScaleZ()
            );
        } else {
            // Scale local translation by parent scale
            float scaledLocalX = bone.getTranslationX() * parentTransform.scaleX();
            float scaledLocalY = bone.getTranslationY() * parentTransform.scaleY();
            float scaledLocalZ = bone.getTranslationZ() * parentTransform.scaleZ();

            // Rotate scaled local translation by parent rotation
            float[] rotatedOffset = parentTransform.rotation().rotateVector(scaledLocalX, scaledLocalY, scaledLocalZ);

            float worldX = parentTransform.translationX() + rotatedOffset[0];
            float worldY = parentTransform.translationY() + rotatedOffset[1];
            float worldZ = parentTransform.translationZ() + rotatedOffset[2];

            QuaternionRotation worldRot = parentTransform.rotation().multiply(localRot);

            float worldScaleX = parentTransform.scaleX() * bone.getScaleX();
            float worldScaleY = parentTransform.scaleY() * bone.getScaleY();
            float worldScaleZ = parentTransform.scaleZ() * bone.getScaleZ();

            currentTransform = new ComputedBoneTransform(
                    bone.getBoneName(),
                    worldX,
                    worldY,
                    worldZ,
                    worldRot,
                    worldScaleX,
                    worldScaleY,
                    worldScaleZ
            );
        }

        results.put(bone.getBoneName(), currentTransform);

        // Evaluate children
        List<String> children = childrenMap.get(bone.getBoneName());
        if (children != null) {
            for (int i = 0; i < children.size(); i++) {
                BoneNode child = bones.get(children.get(i));
                if (child != null) {
                    evaluateSubtree(child, currentTransform, results);
                }
            }
        }
    }

    public synchronized BoneNode getBone(String boneName) {
        return bones.get(boneName);
    }

    public synchronized Map<String, BoneNode> getAllBones() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(bones));
    }

    public synchronized int getBoneCount() {
        return bones.size();
    }
}
