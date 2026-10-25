package dev.vessel.kernel.transform.mixin;

/**
 * Outcome status indicating whether a Mixin @Shadow field target exists and conforms to expectations.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum ShadowValidationStatus {

    /**
     * Target field exists with matching name and descriptor.
     */
    VALID,

    /**
     * Target field was completely absent in the class hierarchy.
     */
    MISSING_FIELD,

    /**
     * Target field exists with the expected name but has an incompatible type descriptor.
     */
    DESCRIPTOR_MISMATCH,

    /**
     * Multiple ambiguous fields matched loose lookup constraints.
     */
    AMBIGUOUS_MATCH
}
