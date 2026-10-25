package dev.vessel.kernel.test;

import dev.vessel.kernel.identity.SourceLoaderKind;
import dev.vessel.kernel.identity.VesselModuleIdentifier;
import dev.vessel.kernel.matrix.CompatibilityAssertionRule;
import dev.vessel.kernel.matrix.CompatibilityVerdict;
import dev.vessel.kernel.matrix.CrossLoaderCompatibilityMatrix;
import dev.vessel.kernel.matrix.MatrixStressReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test suite for Full Cross-Loader Compatibility Matrix (Phase 23).
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class CompatibilityMatrixTest {

    @Test
    @DisplayName("CompatibilityAssertionRule evaluates custom pairwise rules")
    void testCompatibilityAssertionRule() {
        CompatibilityAssertionRule rule = new CompatibilityAssertionRule(
                "TEST-RULE",
                "Verifies identical version",
                (a, b) -> a.version().equals(b.version()) ? CompatibilityVerdict.PASS : CompatibilityVerdict.PASS_WITH_WARNING
        );

        VesselModuleIdentifier modA = VesselModuleIdentifier.of(SourceLoaderKind.FABRIC, "mod_a", "1.0.0");
        VesselModuleIdentifier modB = VesselModuleIdentifier.of(SourceLoaderKind.FORGE, "mod_b", "1.0.0");
        VesselModuleIdentifier modC = VesselModuleIdentifier.of(SourceLoaderKind.NEOFORGE, "mod_c", "2.0.0");

        assertThat(rule.evaluate(modA, modB)).isEqualTo(CompatibilityVerdict.PASS);
        assertThat(rule.evaluate(modA, modC)).isEqualTo(CompatibilityVerdict.PASS_WITH_WARNING);
    }

    @Test
    @DisplayName("CrossLoaderCompatibilityMatrix evaluates multi-loader ecosystem with Decision 7 coexistence")
    void testCrossLoaderMatrixCoexistence() {
        CrossLoaderCompatibilityMatrix matrix = new CrossLoaderCompatibilityMatrix();

        List<VesselModuleIdentifier> modules = List.of(
                VesselModuleIdentifier.of(SourceLoaderKind.FABRIC, "sodium", "0.5.8"),
                VesselModuleIdentifier.of(SourceLoaderKind.FORGE, "jei", "15.2.0"),
                VesselModuleIdentifier.of(SourceLoaderKind.PAPER, "worldedit", "7.2.15"),
                VesselModuleIdentifier.of(SourceLoaderKind.FABRIC, "worldedit", "7.2.15") // Decision 7: cross-loader same namespace
        );

        MatrixStressReport report = matrix.evaluateMatrix(modules);

        assertThat(report.isSuccessful()).isTrue();
        assertThat(report.totalEvaluations()).isEqualTo(6); // 4 choose 2 = 6
        assertThat(report.warningCount()).isEqualTo(1); // (paper:worldedit vs fabric:worldedit)
        assertThat(report.failCriticalCount()).isEqualTo(0);
        assertThat(report.passCount()).isEqualTo(5);
        assertThat(report.getPassRatePercent()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("CrossLoaderCompatibilityMatrix catches Decision 8 duplicate collision within same loader")
    void testCrossLoaderMatrixCollisionDetection() {
        CrossLoaderCompatibilityMatrix matrix = new CrossLoaderCompatibilityMatrix();

        List<VesselModuleIdentifier> modules = List.of(
                VesselModuleIdentifier.of(SourceLoaderKind.FABRIC, "sodium", "0.5.8"),
                VesselModuleIdentifier.of(SourceLoaderKind.FABRIC, "sodium", "0.5.9") // Collision!
        );

        MatrixStressReport report = matrix.evaluateMatrix(modules);

        assertThat(report.isSuccessful()).isFalse();
        assertThat(report.failCriticalCount()).isEqualTo(1);
        assertThat(report.getPassRatePercent()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("CrossLoaderCompatibilityMatrix executes 4,950 pairwise checks under 50ms")
    void testCrossLoaderMatrixStressPerformance() {
        CrossLoaderCompatibilityMatrix matrix = new CrossLoaderCompatibilityMatrix();

        List<VesselModuleIdentifier> modules = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            SourceLoaderKind loader = (i % 2 == 0) ? SourceLoaderKind.FABRIC : SourceLoaderKind.FORGE;
            modules.add(VesselModuleIdentifier.of(loader, "module_" + i, "1.0." + i));
        }

        MatrixStressReport report = matrix.evaluateMatrix(modules);

        // 100 * 99 / 2 = 4,950 evaluations
        assertThat(report.totalEvaluations()).isEqualTo(4950);
        assertThat(report.isSuccessful()).isTrue();
        assertThat(report.elapsedNanos()).isLessThan(50_000_000L); // Under 50ms
    }
}
