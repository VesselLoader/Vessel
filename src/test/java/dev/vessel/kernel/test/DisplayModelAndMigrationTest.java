package dev.vessel.kernel.test;

import dev.vessel.kernel.display.model.BoneTransformState;
import dev.vessel.kernel.display.model.CompactModelMeshDefinition;
import dev.vessel.kernel.display.model.VirtualDisplayEntityBridge;
import dev.vessel.kernel.identity.SourceLoaderKind;
import dev.vessel.kernel.migration.CrossPlatformDataMigrator;
import dev.vessel.kernel.migration.ServerPlatformDetector;
import dev.vessel.kernel.migration.ServerPlatformKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test suite for Display/Model representation (Phase 16) and Server Migration Engine (Phase 22).
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class DisplayModelAndMigrationTest {

    @Test
    @DisplayName("CompactModelMeshDefinition validates coordinates and computes 3D AABB bounding box")
    void testCompactModelMeshDefinition() {
        float[] vertices = new float[]{
                -1.0f, 0.0f, -0.5f,
                1.0f, 2.0f, 0.5f
        };
        float[] uvs = new float[]{
                0.0f, 0.0f,
                1.0f, 1.0f
        };
        int[] indices = new int[]{0, 1};

        CompactModelMeshDefinition mesh = new CompactModelMeshDefinition("custom_sword", vertices, uvs, indices);

        assertThat(mesh.getModelId()).isEqualTo("custom_sword");
        assertThat(mesh.getVertexCount()).isEqualTo(2);
        assertThat(mesh.getIndexCount()).isEqualTo(2);

        float[] aabb = mesh.computeBoundingBox();
        assertThat(aabb).containsExactly(-1.0f, 0.0f, -0.5f, 1.0f, 2.0f, 0.5f);

        // Validation of non-multiple arrays
        assertThatThrownBy(() -> new CompactModelMeshDefinition("bad", new float[]{1.0f, 2.0f}, uvs, indices))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("BoneTransformState accurately sets dirty bitmask upon property changes")
    void testBoneTransformStateDirtyMask() {
        BoneTransformState bone = new BoneTransformState("right_arm");
        assertThat(bone.isDirty()).isFalse();

        bone.setTranslation(0.5f, 1.2f, -0.3f);
        assertThat(bone.isDirty()).isTrue();
        assertThat(bone.hasDirtyFlag(BoneTransformState.DIRTY_TRANSLATION)).isTrue();
        assertThat(bone.hasDirtyFlag(BoneTransformState.DIRTY_ROTATION)).isFalse();

        bone.setRotation(0.1f, 0.2f, 0.0f);
        assertThat(bone.hasDirtyFlag(BoneTransformState.DIRTY_ROTATION)).isTrue();

        bone.setScale(1.5f, 1.5f, 1.5f);
        assertThat(bone.hasDirtyFlag(BoneTransformState.DIRTY_SCALE)).isTrue();

        // Clear dirty
        bone.clearDirty();
        assertThat(bone.isDirty()).isFalse();
        assertThat(bone.getDirtyMask()).isEqualTo(0);
    }

    @Test
    @DisplayName("VirtualDisplayEntityBridge binds transform state and custom rendering properties")
    void testVirtualDisplayEntityBridge() {
        BoneTransformState bone = new BoneTransformState("torso");
        VirtualDisplayEntityBridge bridge = new VirtualDisplayEntityBridge("torso_display", bone);

        assertThat(bridge.getVirtualEntityId()).isGreaterThanOrEqualTo(1_000_000);
        assertThat(bridge.getPartName()).isEqualTo("torso_display");
        assertThat(bridge.getInterpolationDurationTicks()).isEqualTo(3);

        bridge.setInterpolationDurationTicks(5);
        bridge.setViewRange(128.0f);
        bridge.setGlowing(true);

        assertThat(bridge.getInterpolationDurationTicks()).isEqualTo(5);
        assertThat(bridge.getViewRange()).isEqualTo(128.0f);
        assertThat(bridge.isGlowing()).isTrue();
    }

    @Test
    @DisplayName("ServerPlatformDetector identifies legacy platform layouts")
    void testServerPlatformDetector() {
        ServerPlatformDetector detector = new ServerPlatformDetector();

        assertThat(detector.detectFromRootFiles(Set.of("folia.yml", "plugins", "server.properties")))
                .isEqualTo(ServerPlatformKind.FOLIA);

        assertThat(detector.detectFromRootFiles(Set.of("paper.yml", "plugins", "world")))
                .isEqualTo(ServerPlatformKind.PAPER);

        assertThat(detector.detectFromRootFiles(Set.of("bukkit.yml", "plugins")))
                .isEqualTo(ServerPlatformKind.BUKKIT_SPIGOT);

        assertThat(detector.detectFromRootFiles(Set.of("mods", "fabric-server-launch.jar")))
                .isEqualTo(ServerPlatformKind.FABRIC);

        assertThat(detector.detectFromRootFiles(Set.of("mods", "neoforge-server.jar")))
                .isEqualTo(ServerPlatformKind.NEOFORGE);

        assertThat(detector.detectFromRootFiles(Set.of("mods", "serverconfig")))
                .isEqualTo(ServerPlatformKind.FORGE);
    }

    @Test
    @DisplayName("CrossPlatformDataMigrator resolves partitioned layout according to Decision 6")
    void testCrossPlatformDataMigrator() {
        CrossPlatformDataMigrator migrator = new CrossPlatformDataMigrator();

        assertThat(migrator.resolveTargetDataDirectory(SourceLoaderKind.FABRIC, "sodium"))
                .isEqualTo("data/fabric/sodium/");
        assertThat(migrator.resolveTargetDataDirectory(SourceLoaderKind.FORGE, "jei"))
                .isEqualTo("data/forge/jei/");
        assertThat(migrator.resolveTargetDataDirectory(SourceLoaderKind.PAPER, "worldedit"))
                .isEqualTo("data/plugins/worldedit/");

        assertThat(migrator.requiresMigration("data/mods/sodium/config.json")).isTrue();
        assertThat(migrator.requiresMigration("plugins/WorldEdit/config.yml")).isTrue();
        assertThat(migrator.requiresMigration("data/fabric/sodium/config.json")).isFalse();
    }
}
