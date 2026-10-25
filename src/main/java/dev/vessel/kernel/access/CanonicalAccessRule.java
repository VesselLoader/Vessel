package dev.vessel.kernel.access;

import java.util.Objects;

/**
 * Immutable carrier representing a single unified access modification rule.
 *
 * @param owner internal class name (e.g. "net/minecraft/world/entity/Entity")
 * @param memberName member name (or empty string for class targets)
 * @param memberDescriptor member descriptor (or empty string for class/field targets where absent)
 * @param modification the applied modification rule
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record CanonicalAccessRule(
        String owner,
        String memberName,
        String memberDescriptor,
        CanonicalAccessModification modification
) {
    public CanonicalAccessRule {
        Objects.requireNonNull(owner, "owner cannot be null");
        Objects.requireNonNull(memberName, "memberName cannot be null");
        Objects.requireNonNull(memberDescriptor, "memberDescriptor cannot be null");
        Objects.requireNonNull(modification, "modification cannot be null");
    }
}
