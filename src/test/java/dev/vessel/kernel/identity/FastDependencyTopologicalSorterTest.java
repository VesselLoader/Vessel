package dev.vessel.kernel.identity;

import dev.vessel.kernel.identity.dependency.DependencyConstraintKind;
import dev.vessel.kernel.identity.dependency.ModuleDependencyRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test suite for {@link FastDependencyTopologicalSorter}.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
class FastDependencyTopologicalSorterTest {

    private final FastDependencyTopologicalSorter sorter = new FastDependencyTopologicalSorter();

    @Test
    @DisplayName("Sorts linear dependency chain correctly")
    void testLinearDependencies() {
        VesselModuleIdentifier modA = new VesselModuleIdentifier(SourceLoaderKind.FABRIC, "mod-a", "1.0.0", ModuleClassifier.MAIN);
        VesselModuleIdentifier modB = new VesselModuleIdentifier(SourceLoaderKind.FABRIC, "mod-b", "1.0.0", ModuleClassifier.MAIN);
        VesselModuleIdentifier modC = new VesselModuleIdentifier(SourceLoaderKind.FABRIC, "mod-c", "1.0.0", ModuleClassifier.MAIN);

        // modA requires modB, modB requires modC
        List<ModuleDependencyRule> rules = List.of(
                new ModuleDependencyRule(modA, "mod-b", "1.0.0", DependencyConstraintKind.MANDATORY),
                new ModuleDependencyRule(modB, "mod-c", "1.0.0", DependencyConstraintKind.MANDATORY)
        );

        List<VesselModuleIdentifier> sorted = sorter.sort(List.of(modA, modB, modC), rules);

        assertThat(sorted).containsExactly(modC, modB, modA);
    }

    @Test
    @DisplayName("Sorts diamond dependency graph correctly")
    void testDiamondDependencies() {
        VesselModuleIdentifier root = new VesselModuleIdentifier(SourceLoaderKind.NEOFORGE, "core", "1.0.0", ModuleClassifier.MAIN);
        VesselModuleIdentifier left = new VesselModuleIdentifier(SourceLoaderKind.NEOFORGE, "left-ext", "1.0.0", ModuleClassifier.MAIN);
        VesselModuleIdentifier right = new VesselModuleIdentifier(SourceLoaderKind.NEOFORGE, "right-ext", "1.0.0", ModuleClassifier.MAIN);
        VesselModuleIdentifier top = new VesselModuleIdentifier(SourceLoaderKind.NEOFORGE, "app", "1.0.0", ModuleClassifier.MAIN);

        List<ModuleDependencyRule> rules = List.of(
                new ModuleDependencyRule(left, "core", "1.0.0", DependencyConstraintKind.MANDATORY),
                new ModuleDependencyRule(right, "core", "1.0.0", DependencyConstraintKind.MANDATORY),
                new ModuleDependencyRule(top, "left-ext", "1.0.0", DependencyConstraintKind.MANDATORY),
                new ModuleDependencyRule(top, "right-ext", "1.0.0", DependencyConstraintKind.MANDATORY)
        );

        List<VesselModuleIdentifier> sorted = sorter.sort(List.of(top, left, right, root), rules);

        assertThat(sorted.get(0)).isEqualTo(root);
        assertThat(sorted.get(3)).isEqualTo(top);
        assertThat(sorted.subList(1, 3)).containsExactlyInAnyOrder(left, right);
    }

    @Test
    @DisplayName("Deterministically resolves soft/ordering hint dependency cycles")
    void testSoftCycleResolution() {
        VesselModuleIdentifier modA = new VesselModuleIdentifier(SourceLoaderKind.FABRIC, "mod-a", "1.0.0", ModuleClassifier.MAIN);
        VesselModuleIdentifier modB = new VesselModuleIdentifier(SourceLoaderKind.FABRIC, "mod-b", "1.0.0", ModuleClassifier.MAIN);

        // modA requires modB MANDATORY; modB has LOAD_AFTER modA (soft hint creating a cycle)
        List<ModuleDependencyRule> rules = List.of(
                new ModuleDependencyRule(modA, "mod-b", "1.0.0", DependencyConstraintKind.MANDATORY),
                new ModuleDependencyRule(modB, "mod-a", "1.0.0", DependencyConstraintKind.LOAD_AFTER)
        );

        // Should resolve the cycle by breaking the soft LOAD_AFTER rule and keeping MANDATORY rule
        List<VesselModuleIdentifier> sorted = sorter.sort(List.of(modA, modB), rules);
        assertThat(sorted).containsExactly(modB, modA);
    }

    @Test
    @DisplayName("Throws CircularDependencyException when unresolvable hard cycle exists")
    void testHardCycleDetection() {
        VesselModuleIdentifier modA = new VesselModuleIdentifier(SourceLoaderKind.FORGE, "mod-a", "1.0.0", ModuleClassifier.MAIN);
        VesselModuleIdentifier modB = new VesselModuleIdentifier(SourceLoaderKind.FORGE, "mod-b", "1.0.0", ModuleClassifier.MAIN);

        List<ModuleDependencyRule> rules = List.of(
                new ModuleDependencyRule(modA, "mod-b", "1.0.0", DependencyConstraintKind.MANDATORY),
                new ModuleDependencyRule(modB, "mod-a", "1.0.0", DependencyConstraintKind.MANDATORY)
        );

        assertThatThrownBy(() -> sorter.sort(List.of(modA, modB), rules))
                .isInstanceOf(CircularDependencyException.class)
                .hasMessageContaining("Circular dependency detected");
    }

    @Test
    @DisplayName("Throws IllegalStateException when mandatory dependency is missing")
    void testMissingMandatoryDependency() {
        VesselModuleIdentifier modA = new VesselModuleIdentifier(SourceLoaderKind.BUKKIT, "plugin-a", "1.0.0", ModuleClassifier.MAIN);
        List<ModuleDependencyRule> rules = List.of(
                new ModuleDependencyRule(modA, "missing-lib", "1.0.0", DependencyConstraintKind.MANDATORY)
        );

        assertThatThrownBy(() -> sorter.sort(List.of(modA), rules))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing mandatory dependency: missing-lib");
    }
}
