package dev.vessel.kernel.matrix;

/**
 * Outcome verdict for cross-loader compatibility matrix evaluation.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum CompatibilityVerdict {

    /** Modules are 100% compatible with zero conflicts or warnings. */
    PASS,

    /** Modules are compatible but operate under a documented usage caveat (e.g. Decision 7 cross-loader namespace coexistence). */
    PASS_WITH_WARNING,

    /** Unrecoverable conflict requiring hard stop (e.g. Decision 8 duplicate namespace within same loader). */
    FAIL_CRITICAL,

    /** Test skipped due to unmet environmental preconditions. */
    SKIPPED
}
