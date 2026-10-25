package dev.vessel.kernel.access;

import java.util.Objects;

/**
 * Immutable carrier defining the desired visibility and mutability modifications for a target.
 *
 * @param targetKind whether this rule applies to class, method, or field
 * @param accessLevel target visibility level
 * @param makeExtendable whether to remove final/sealed modifiers from classes or methods
 * @param makeMutable whether to remove final modifier from fields
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record CanonicalAccessModification(
        AccessTargetKind targetKind,
        CanonicalAccessLevel accessLevel,
        boolean makeExtendable,
        boolean makeMutable
) {
    public CanonicalAccessModification {
        Objects.requireNonNull(targetKind, "targetKind cannot be null");
        Objects.requireNonNull(accessLevel, "accessLevel cannot be null");
    }
}
