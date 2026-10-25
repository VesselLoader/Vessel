package dev.vessel.kernel.test;

import dev.vessel.kernel.identity.SourceLoaderKind;
import dev.vessel.kernel.identity.VesselModuleIdentifier;
import dev.vessel.kernel.identity.dependency.DependencyConstraintKind;
import dev.vessel.kernel.identity.dependency.FastDependencyTopologicalSorter;
import dev.vessel.kernel.identity.dependency.ModuleDependencyRule;
import dev.vessel.kernel.optimization.entity.EntityBrainTickThrottle;
import dev.vessel.kernel.optimization.entity.EntityMemoryCompactTable;
import dev.vessel.kernel.resourcepack.atlas.FastTextureAtlasPacker;
import dev.vessel.kernel.resourcepack.atlas.TextureAtlasTileDescriptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test suite for Universal Dependency Topological Sorter (Phase 2),
 * Resource Pack Atlas Packer (Phase 14), and Entity Memory Optimization (Phase 11).
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class DependencyAtlasAndEntityOptimizationTest {

    @Test
    @DisplayName("FastDependencyTopologicalSorter orders modules linearly according to dependencies")
    void testFastDependencyTopologicalSorterLinear() {
        FastDependencyTopologicalSorter sorter = new FastDependencyTopologicalSorter();

        VesselModuleIdentifier modA = VesselModuleIdentifier.of(SourceLoaderKind.FABRIC, "mod_a", "1.0.0");
        VesselModuleIdentifier modB = VesselModuleIdentifier.of(SourceLoaderKind.FABRIC, "mod_b", "1.0.0");
        VesselModuleIdentifier modC = VesselModuleIdentifier.of(SourceLoaderKind.FABRIC, "mod_c", "1.0.0");

        // C requires B; B requires A => Expected order: A, B, C
        List<ModuleDependencyRule> rules = List.of(
                new ModuleDependencyRule(modC, "mod_b", "*", DependencyConstraintKind.MANDATORY),
                new ModuleDependencyRule(modB, "mod_a", "*", DependencyConstraintKind.MANDATORY)
        );

        List<VesselModuleIdentifier> sorted = sorter.sort(List.of(modC, modB, modA), rules);

        assertThat(sorted).containsExactly(modA, modB, modC);
    }

    @Test
    @DisplayName("FastDependencyTopologicalSorter detects circular dependencies and throws")
    void testFastDependencyTopologicalSorterCycleDetection() {
        FastDependencyTopologicalSorter sorter = new FastDependencyTopologicalSorter();

        VesselModuleIdentifier modA = VesselModuleIdentifier.of(SourceLoaderKind.FORGE, "mod_alpha", "1.0.0");
        VesselModuleIdentifier modB = VesselModuleIdentifier.of(SourceLoaderKind.FORGE, "mod_beta", "1.0.0");

        // A requires B; B requires A => Circular cycle
        List<ModuleDependencyRule> rules = List.of(
                new ModuleDependencyRule(modA, "mod_beta", "*", DependencyConstraintKind.MANDATORY),
                new ModuleDependencyRule(modB, "mod_alpha", "*", DependencyConstraintKind.MANDATORY)
        );

        assertThatThrownBy(() -> sorter.sort(List.of(modA, modB), rules))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Circular dependency detected");
    }

    @Test
    @DisplayName("FastTextureAtlasPacker packs multiple textures into a power-of-two composite atlas")
    void testFastTextureAtlasPacker() {
        FastTextureAtlasPacker packer = new FastTextureAtlasPacker(1024);

        TextureAtlasTileDescriptor tile1 = new TextureAtlasTileDescriptor("item/sword", 16, 16, false);
        TextureAtlasTileDescriptor tile2 = new TextureAtlasTileDescriptor("block/dirt", 32, 32, false);
        TextureAtlasTileDescriptor tile3 = new TextureAtlasTileDescriptor("entity/banner", 16, 32, false);
        TextureAtlasTileDescriptor tile4 = new TextureAtlasTileDescriptor("gui/widget", 64, 64, false);

        List<TextureAtlasTileDescriptor> tiles = List.of(tile1, tile2, tile3, tile4);
        int atlasSize = packer.pack(tiles);

        assertThat(atlasSize).isGreaterThanOrEqualTo(64);
        assertThat((atlasSize & (atlasSize - 1))).isEqualTo(0); // Power of two check

        for (TextureAtlasTileDescriptor t : tiles) {
            assertThat(t.isAllocated()).isTrue();
            assertThat(t.getAllocatedX() + t.getWidth()).isLessThanOrEqualTo(atlasSize);
            assertThat(t.getAllocatedY() + t.getHeight()).isLessThanOrEqualTo(atlasSize);
        }
    }

    @Test
    @DisplayName("EntityMemoryCompactTable manages primitive memory slots without heap allocations")
    void testEntityMemoryCompactTable() {
        EntityMemoryCompactTable table = new EntityMemoryCompactTable();

        table.setMemory(101, 500L); // type 101 expires at tick 500
        table.setMemory(102, 200L); // type 102 expires at tick 200

        assertThat(table.hasMemory(101, 100L)).isTrue();
        assertThat(table.hasMemory(102, 100L)).isTrue();
        assertThat(table.hasMemory(102, 250L)).isFalse(); // Expired!

        int purged = table.purgeExpired(250L);
        assertThat(purged).isEqualTo(1);
        assertThat(table.hasMemory(102, 100L)).isFalse();
        assertThat(table.hasMemory(101, 100L)).isTrue();

        table.clearMemory(101);
        assertThat(table.hasMemory(101, 100L)).isFalse();
        assertThat(table.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("EntityBrainTickThrottle staggers AI execution based on player distance")
    void testEntityBrainTickThrottle() {
        EntityBrainTickThrottle throttle = new EntityBrainTickThrottle(16.0, 32.0, 64.0);

        // Close entity (distance = 10 blocks -> d^2 = 100 <= 256)
        assertThat(throttle.computeTickInterval(100.0)).isEqualTo(1);
        assertThat(throttle.shouldTick(42, 1001L, 100.0)).isTrue();
        assertThat(throttle.shouldTick(42, 1002L, 100.0)).isTrue();

        // Medium distance (distance = 25 blocks -> d^2 = 625 <= 1024)
        assertThat(throttle.computeTickInterval(625.0)).isEqualTo(2);

        // Distant mob (distance = 100 blocks -> d^2 = 10000 > 4096)
        assertThat(throttle.computeTickInterval(10000.0)).isEqualTo(10);
    }
}
