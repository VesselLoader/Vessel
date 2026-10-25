package dev.vessel.kernel.access;

/**
 * Kind of JVM bytecode element targeted by a canonical access modification rule.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum AccessTargetKind {

    /**
     * Target represents a class or interface type.
     */
    CLASS,

    /**
     * Target represents a class field member.
     */
    FIELD,

    /**
     * Target represents a class or interface method member.
     */
    METHOD
}
