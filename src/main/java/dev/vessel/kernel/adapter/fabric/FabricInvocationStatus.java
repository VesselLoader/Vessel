package dev.vessel.kernel.adapter.fabric;

/**
 * Enumeration representing the execution outcome of a Fabric mod entrypoint invocation.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum FabricInvocationStatus {

    /**
     * Entrypoint executed successfully without errors.
     */
    SUCCESS,

    /**
     * Entrypoint invocation failed due to an uncaught exception.
     */
    FAILED,

    /**
     * Entrypoint was intentionally skipped (e.g. client-only entrypoint on dedicated server).
     */
    SKIPPED,

    /**
     * Specified entrypoint class or method was not found in the target classloader.
     */
    NOT_FOUND
}
