package dev.vessel.kernel.identity.dependency;

/**
 * Expresses the semantic strength and sequencing intent of an inter-module dependency.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum DependencyConstraintKind {

    /** Hard requirement; target module must be present and active. */
    MANDATORY,

    /** Optional enhancement; activates features if target module is loaded. */
    OPTIONAL,

    /** Hard incompatibility; server refuses to load if target module is present. */
    CONFLICTS,

    /** Sequencing hint: this module must load before target module if both exist. */
    LOAD_BEFORE,

    /** Sequencing hint: this module must load after target module if both exist. */
    LOAD_AFTER
}
