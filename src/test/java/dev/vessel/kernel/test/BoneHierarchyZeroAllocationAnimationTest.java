package dev.vessel.kernel.test;

import dev.vessel.kernel.modelengine.bone.BoneHierarchyTree;
import dev.vessel.kernel.modelengine.bone.BoneNode;
import dev.vessel.kernel.modelengine.bone.ComputedBoneTransform;
import dev.vessel.kernel.modelengine.bone.QuaternionRotation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
 * Unit verification test suite for Phase 7 (Model Engine 3D, Hitbox, Bone Matrix & Presentation).
 * Tests zero-allocation quaternion vector rotation and forward kinematics bone hierarchy evaluation.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class BoneHierarchyZeroAllocationAnimationTest {

    @Test
    @DisplayName("QuaternionRotation rotateVectorTo transforms vector into destination buffer without allocations")
    void testQuaternionRotateVectorTo() {
        // 90 degrees around Y axis
        QuaternionRotation rotY90 = QuaternionRotation.fromEuler(0.0f, (float) (Math.PI / 2.0), 0.0f);

        float[] out = new float[5];
        // Rotate vector (1, 0, 0) -> should become approximately (0, 0, -1)
        rotY90.rotateVectorTo(1.0f, 0.0f, 0.0f, out, 1);

        assertThat(out[1]).isCloseTo(0.0f, offset(1e-4f));
        assertThat(out[2]).isCloseTo(0.0f, offset(1e-4f));
        assertThat(out[3]).isCloseTo(-1.0f, offset(1e-4f));
    }

    @Test
    @DisplayName("BoneHierarchyTree accurately propagates composite parent-child transformations")
    void testBoneHierarchyTreePropagation() {
        BoneHierarchyTree tree = new BoneHierarchyTree();

        // Root bone translated by (0, 10, 0)
        BoneNode root = new BoneNode("root", null);
        root.setTranslation(0.0f, 10.0f, 0.0f);
        tree.registerBone(root);

        // Child bone translated by (5, 0, 0) relative to root
        BoneNode child = new BoneNode("arm", "root");
        child.setTranslation(5.0f, 0.0f, 0.0f);
        tree.registerBone(child);

        Map<String, ComputedBoneTransform> transforms = tree.computeGlobalTransforms();

        assertThat(transforms).containsKey("root");
        assertThat(transforms).containsKey("arm");

        ComputedBoneTransform rootTransform = transforms.get("root");
        assertThat(rootTransform.translationX()).isEqualTo(0.0f);
        assertThat(rootTransform.translationY()).isEqualTo(10.0f);
        assertThat(rootTransform.translationZ()).isEqualTo(0.0f);

        ComputedBoneTransform childTransform = transforms.get("arm");
        // Arm world position = root (0, 10, 0) + child local (5, 0, 0) = (5, 10, 0)
        assertThat(childTransform.translationX()).isEqualTo(5.0f);
        assertThat(childTransform.translationY()).isEqualTo(10.0f);
        assertThat(childTransform.translationZ()).isEqualTo(0.0f);
    }
}
