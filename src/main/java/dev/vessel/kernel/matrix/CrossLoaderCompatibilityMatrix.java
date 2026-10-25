package dev.vessel.kernel.matrix;

import dev.vessel.kernel.identity.VesselModuleIdentifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Coordination matrix for cross-loader compatibility verification and automated stress testing.
 * Runs pairwise assertions across registered modules to detect collisions, lifecycle incompatibilities,
 * and ensure clean coexistence across Fabric, Quilt, Forge, NeoForge, and Bukkit.
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class CrossLoaderCompatibilityMatrix {

    private final List<CompatibilityAssertionRule> rules = new CopyOnWriteArrayList<>();

    public CrossLoaderCompatibilityMatrix() {
        registerStockVesselRules();
    }

    /**
     * Registers a custom compatibility assertion rule into the matrix.
     *
     * @param rule the rule to register
     */
    public void addRule(CompatibilityAssertionRule rule) {
        Objects.requireNonNull(rule, "rule cannot be null");
        rules.add(rule);
    }

    /**
     * Executes pairwise compatibility verification across a list of modules.
     *
     * @param modules the modules to evaluate
     * @return MatrixStressReport summarizing outcomes
     */
    public MatrixStressReport evaluateMatrix(List<VesselModuleIdentifier> modules) {
        Objects.requireNonNull(modules, "modules cannot be null");

        long startNanos = System.nanoTime();
        int totalEvaluations = 0;
        int passCount = 0;
        int warningCount = 0;
        int failCriticalCount = 0;

        int size = modules.size();
        CompatibilityAssertionRule[] ruleArray = rules.toArray(new CompatibilityAssertionRule[0]);
        int ruleCount = ruleArray.length;

        for (int i = 0; i < size; i++) {
            VesselModuleIdentifier modA = modules.get(i);
            for (int j = i + 1; j < size; j++) {
                VesselModuleIdentifier modB = modules.get(j);

                for (int r = 0; r < ruleCount; r++) {
                    totalEvaluations++;
                    CompatibilityVerdict verdict = ruleArray[r].evaluate(modA, modB);

                    switch (verdict) {
                        case PASS -> passCount++;
                        case PASS_WITH_WARNING -> warningCount++;
                        case FAIL_CRITICAL -> failCriticalCount++;
                        case SKIPPED -> {}
                    }
                }
            }
        }

        long elapsedNanos = System.nanoTime() - startNanos;
        return new MatrixStressReport(
                totalEvaluations,
                passCount,
                warningCount,
                failCriticalCount,
                elapsedNanos
        );
    }

    private void registerStockVesselRules() {
        // Rule 1: Decision 7 & 8 Identity Collision Rule
        rules.add(new CompatibilityAssertionRule(
                "VESSEL-DEC-7-8",
                "Enforces Decision 7 (coexistence across different loaders) and Decision 8 (hard stop within same loader)",
                (a, b) -> {
                    if (a.namespace().equals(b.namespace())) {
                        if (a.loader() == b.loader()) {
                            return CompatibilityVerdict.FAIL_CRITICAL;
                        } else {
                            return CompatibilityVerdict.PASS_WITH_WARNING;
                        }
                    }
                    return CompatibilityVerdict.PASS;
                }
        ));
    }

    public int getRuleCount() {
        return rules.size();
    }
}
