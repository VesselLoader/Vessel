package dev.vessel.kernel.runtime;

/**
 * Lifecycle execution states for a VESSEL modular execution container.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum ModuleExecutionState {

    /** Container instantiated, metadata verified, ClassLoader ready. */
    INITIALIZED,

    /** Classes and dependencies currently being resolved and linked. */
    LOADING,

    /** Module is actively running, event listeners registered, hooks bound. */
    ACTIVE,

    /** Module has been cleanly disabled or unloaded. */
    DISABLED,

    /** Module encountered an unrecoverable failure during bootstrap or runtime. */
    FAILED
}
