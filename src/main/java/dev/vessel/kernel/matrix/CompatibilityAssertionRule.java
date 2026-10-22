package dev.vessel.kernel.matrix;

import dev.vessel.kernel.identity.VesselModuleIdentifier;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Evaluates a single pairwise compatibility rule between two modules.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class CompatibilityAssertionRule {

    private final String ruleId;
    private final String description;
    private final BiFunction<VesselModuleIdentifier, VesselModuleIdentifier, CompatibilityVerdict> evaluator;

    /**
     * Constructs a CompatibilityAssertionRule.
     *
     * @param ruleId unique rule identifier
     * @param description human-readable explanation of rule contract
     * @param evaluator evaluation logic returning a CompatibilityVerdict
     */
    public CompatibilityAssertionRule(
            String ruleId,
            String description,
            BiFunction<VesselModuleIdentifier, VesselModuleIdentifier, CompatibilityVerdict> evaluator) {
        this.ruleId = Objects.requireNonNull(ruleId, "ruleId cannot be null");
        this.description = Objects.requireNonNull(description, "description cannot be null");
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator cannot be null");
    }

    /**
     * Evaluates the compatibility between two modules according to this rule.
     *
     * @param modA first module
     * @param modB second module
     * @return outcome verdict
     */
    public CompatibilityVerdict evaluate(VesselModuleIdentifier modA, VesselModuleIdentifier modB) {
        return evaluator.apply(modA, modB);
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getDescription() {
        return description;
    }
}
