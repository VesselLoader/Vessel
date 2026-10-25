package dev.vessel.kernel.modelengine;

import dev.vessel.kernel.display.model.BoneTransformState;
import dev.vessel.kernel.display.model.VirtualDisplayEntityBridge;
import dev.vessel.kernel.modelengine.bone.BoneHierarchyTree;
import dev.vessel.kernel.modelengine.bone.BoneNode;
import dev.vessel.kernel.modelengine.bone.ComputedBoneTransform;
import dev.vessel.kernel.modelengine.bone.ItemDisplayRenderer;
import dev.vessel.kernel.modelengine.bone.LegacyArmorStandRenderer;
import dev.vessel.kernel.modelengine.bone.QuaternionRotation;
import dev.vessel.kernel.modelengine.bone.ResourcePackModelMapping;
import dev.vessel.kernel.modelengine.lod.DisplayInterpolationConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
 * Verification test suite for Bone Hierarchies, Quaternion Math, Resource Pack Mapping,
 * and Modern/Legacy renderers.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public class BoneHierarchyAndRenderingTest {

    @Test
    @DisplayName("QuaternionRotation correctly converts Euler angles and rotates 3D vectors")
    void testQuaternionRotationMath() {
        // Identity quaternion
        QuaternionRotation identity = QuaternionRotation.IDENTITY;
        float[] v = identity.rotateVector(1.0f, 2.0f, 3.0f);
        assertThat(v).containsExactly(1.0f, 2.0f, 3.0f);

        // 90 degrees around Y axis (yaw = PI/2)
        QuaternionRotation rotY90 = QuaternionRotation.fromEuler(0.0f, (float) (Math.PI / 2.0), 0.0f);
        float[] rotatedV = rotY90.rotateVector(1.0f, 0.0f, 0.0f);
        // (1, 0, 0) rotated 90 deg around Y points to (0, 0, -1) in standard right-hand system
        assertThat(rotatedV[0]).isCloseTo(0.0f, offset(1e-4f));
        assertThat(rotatedV[1]).isCloseTo(0.0f, offset(1e-4f));
        assertThat(rotatedV[2]).isCloseTo(-1.0f, offset(1e-4f));

        // Multiply: two 90-degree rotations = 180 degrees
        QuaternionRotation rotY180 = rotY90.multiply(rotY90);
        float[] rotated180 = rotY180.rotateVector(1.0f, 0.0f, 0.0f);
        assertThat(rotated180[0]).isCloseTo(-1.0f, offset(1e-4f));
        assertThat(rotated180[2]).isCloseTo(0.0f, offset(1e-4f));
    }

    @Test
    @DisplayName("BoneHierarchyTree propagates forward-kinematics transforms from root to children")
    void testBoneHierarchyTreePropagation() {
        BoneHierarchyTree tree = new BoneHierarchyTree();

        // Root bone (e.g. torso) translated at (0, 1.0, 0)
        BoneNode rootBone = new BoneNode("torso", null);
        rootBone.setTranslation(0.0f, 1.0f, 0.0f);
        rootBone.setCustomModelData(1001);
        tree.registerBone(rootBone);

        // Child bone (e.g. head) offset from torso by (0, 0.8, 0)
        BoneNode headBone = new BoneNode("head", "torso");
        headBone.setTranslation(0.0f, 0.8f, 0.0f);
        headBone.setCustomModelData(1002);
        tree.registerBone(headBone);

        // Child bone (e.g. arm) offset by (0.5, 0.0, 0) with 2.0x scale
        BoneNode armBone = new BoneNode("arm", "torso");
        armBone.setTranslation(0.5f, 0.0f, 0.0f);
        armBone.setScale(2.0f, 2.0f, 2.0f);
        armBone.setCustomModelData(1003);
        tree.registerBone(armBone);

        Map<String, ComputedBoneTransform> transforms = tree.computeGlobalTransforms();

        assertThat(transforms).hasSize(3);

        // Torso world position
        ComputedBoneTransform torsoT = transforms.get("torso");
        assertThat(torsoT.translationX()).isEqualTo(0.0f);
        assertThat(torsoT.translationY()).isEqualTo(1.0f);
        assertThat(torsoT.translationZ()).isEqualTo(0.0f);

        // Head world position = 1.0 + 0.8 = 1.8
        ComputedBoneTransform headT = transforms.get("head");
        assertThat(headT.translationX()).isEqualTo(0.0f);
        assertThat(headT.translationY()).isCloseTo(1.8f, offset(1e-4f));
        assertThat(headT.translationZ()).isEqualTo(0.0f);

        // Arm world position = 0.5 X, 1.0 Y
        ComputedBoneTransform armT = transforms.get("arm");
        assertThat(armT.translationX()).isCloseTo(0.5f, offset(1e-4f));
        assertThat(armT.translationY()).isCloseTo(1.0f, offset(1e-4f));
        assertThat(armT.scaleX()).isEqualTo(2.0f);
    }

    @Test
    @DisplayName("ItemDisplayRenderer configures modern 1.19.4+ ItemDisplay entity properties")
    void testItemDisplayRendererConfiguration() {
        BoneNode bone = new BoneNode("wing_left", "torso");
        bone.setCustomModelData(20050);
        bone.setItemMaterial("minecraft:leather_horse_armor");

        BoneTransformState transformState = new BoneTransformState("wing_left");
        VirtualDisplayEntityBridge bridge = new VirtualDisplayEntityBridge("wing_left", transformState);

        ItemDisplayRenderer renderer = new ItemDisplayRenderer(bone, bridge);

        assertThat(renderer.getCustomModelData()).isEqualTo(20050);
        assertThat(renderer.getItemMaterial()).isEqualTo("minecraft:leather_horse_armor");
        assertThat(bridge.getCustomModelData()).isEqualTo(20050);
        assertThat(bridge.getBillboardConstraint()).isEqualTo(ItemDisplayRenderer.BILLBOARD_FIXED);
        assertThat(bridge.getInterpolationDurationTicks()).isEqualTo(3);

        // Apply transform
        ComputedBoneTransform transform = new ComputedBoneTransform(
                "wing_left",
                1.5f, 2.0f, -0.5f,
                QuaternionRotation.IDENTITY,
                1.0f, 1.0f, 1.0f
        );
        renderer.applyTransform(transform);

        assertThat(transformState.getTranslationX()).isEqualTo(1.5f);
        assertThat(transformState.getTranslationY()).isEqualTo(2.0f);
        assertThat(transformState.getTranslationZ()).isEqualTo(-0.5f);
        assertThat(transformState.isDirty()).isTrue();
    }

    @Test
    @DisplayName("LegacyArmorStandRenderer configures small marker ArmorStand with HeadPose Euler angles")
    void testLegacyArmorStandRendererConfiguration() {
        BoneNode bone = new BoneNode("sword", "arm");
        bone.setCustomModelData(30010);

        LegacyArmorStandRenderer renderer = new LegacyArmorStandRenderer(bone, 7001);

        assertThat(renderer.getArmorStandEntityId()).isEqualTo(7001);
        assertThat(renderer.isSmall()).isTrue();
        assertThat(renderer.isMarker()).isTrue();
        assertThat(renderer.isInvisible()).isTrue();
        assertThat(renderer.getCustomModelData()).isEqualTo(30010);

        // Apply transform with 45 degree pitch
        QuaternionRotation rotPitch45 = QuaternionRotation.fromEuler((float) Math.toRadians(45.0), 0.0f, 0.0f);
        ComputedBoneTransform transform = new ComputedBoneTransform(
                "sword",
                0.2f, 1.1f, 0.4f,
                rotPitch45,
                1.0f, 1.0f, 1.0f
        );
        renderer.applyTransform(transform);

        assertThat(renderer.getWorldX()).isEqualTo(0.2f);
        assertThat(renderer.getWorldY()).isEqualTo(1.1f);
        assertThat(renderer.getWorldZ()).isEqualTo(0.4f);
        assertThat(renderer.getHeadPosePitch()).isCloseTo(45.0f, offset(0.1f));
    }

    @Test
    @DisplayName("ResourcePackModelMapping registers and resolves model CustomModelData and JSON paths")
    void testResourcePackModelMappingRegistry() {
        ResourcePackModelMapping mapping = new ResourcePackModelMapping();

        mapping.registerMapping("custom_golem", "head", 10001, "assets/vessel/models/item/golem_head.json");
        mapping.registerMapping("custom_golem", "body", 10002, "assets/vessel/models/item/golem_body.json");

        assertThat(mapping.getCustomModelData("custom_golem", "head")).isEqualTo(10001);
        assertThat(mapping.getCustomModelData("custom_golem", "body")).isEqualTo(10002);
        assertThat(mapping.getModelJsonPath("custom_golem", "head")).isEqualTo("assets/vessel/models/item/golem_head.json");
        assertThat(mapping.findBoneKeyByCmd(10001)).isEqualTo("custom_golem:head");

        // Non-existent bone returns 0
        assertThat(mapping.getCustomModelData("custom_golem", "nonexistent")).isEqualTo(0);
    }

    @Test
    @DisplayName("QuaternionRotation correctly evaluates Euler angles across compound yaw and roll without axis collapse")
    void testQuaternionEulerAngleRoundTripAndGimbalLock() {
        // Compound rotation: yaw = 90 deg (PI/2), roll = 45 deg (PI/4), pitch = 0
        float expectedYaw = (float) (Math.PI / 2.0);
        float expectedRoll = (float) (Math.PI / 4.0);
        float expectedPitch = 0.0f;

        QuaternionRotation q = QuaternionRotation.fromEuler(expectedPitch, expectedYaw, expectedRoll);
        float[] euler = q.toEulerAngles();

        assertThat(euler[0]).isCloseTo(expectedPitch, offset(1e-4f));
        assertThat(euler[1]).isCloseTo(expectedYaw, offset(1e-4f));
        assertThat(euler[2]).isCloseTo(expectedRoll, offset(1e-4f));

        // Gimbal lock test: pitch at +90 degrees (PI/2)
        QuaternionRotation gimbalLockQ = QuaternionRotation.fromEuler((float) (Math.PI / 2.0), 0.0f, 0.0f);
        float[] gimbalEuler = gimbalLockQ.toEulerAngles();
        assertThat(gimbalEuler[0]).isCloseTo((float) (Math.PI / 2.0), offset(1e-4f));
        assertThat(gimbalEuler[2]).isCloseTo(0.0f, offset(1e-4f));
    }

    @Test
    @DisplayName("ItemDisplayRenderer wires custom DisplayInterpolationConfig duration and teleport ticks")
    void testItemDisplayRendererInterpolationConfig() {
        BoneNode bone = new BoneNode("shield_plate", null);
        bone.setCustomModelData(40001);

        BoneTransformState transformState = new BoneTransformState("shield_plate");
        VirtualDisplayEntityBridge bridge = new VirtualDisplayEntityBridge("shield_plate", transformState);

        DisplayInterpolationConfig config = new DisplayInterpolationConfig(2);
        config.setTeleportDurationTicks(1);

        ItemDisplayRenderer renderer = new ItemDisplayRenderer(bone, bridge, config);

        assertThat(bridge.getInterpolationDurationTicks()).isEqualTo(2);
        assertThat(bridge.getTeleportDurationTicks()).isEqualTo(1);
        assertThat(config.getInterpolationDurationMillis()).isEqualTo(100L);
    }
}
