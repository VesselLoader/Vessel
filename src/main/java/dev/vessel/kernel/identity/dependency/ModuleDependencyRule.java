package dev.vessel.kernel.identity.dependency;

import dev.vessel.kernel.identity.VesselModuleIdentifier;

import java.util.Objects;

/**
 * Immutable declaration of a module dependency constraint.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record ModuleDependencyRule(
        VesselModuleIdentifier sourceModule,
        String targetNamespace,
        String versionConstraint,
        DependencyConstraintKind kind
) {
    public ModuleDependencyRule {
        Objects.requireNonNull(sourceModule, "sourceModule cannot be null");
        Objects.requireNonNull(targetNamespace, "targetNamespace cannot be null");
        Objects.requireNonNull(versionConstraint, "versionConstraint cannot be null");
        Objects.requireNonNull(kind, "kind cannot be null");

        targetNamespace = targetNamespace.trim().toLowerCase();
        if (targetNamespace.isEmpty()) {
            throw new IllegalArgumentException("targetNamespace cannot be empty");
        }
    }
}
