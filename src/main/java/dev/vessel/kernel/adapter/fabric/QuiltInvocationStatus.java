package dev.vessel.kernel.adapter.fabric;

/**
 * Enumeration representing the execution status of a Quilt mod entrypoint invocation.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum QuiltInvocationStatus {

    /**
     * Entrypoint executed successfully.
     */
    SUCCESS,

    /**
     * Entrypoint invocation threw an unhandled exception.
     */
    FAILED,

    /**
     * Entrypoint was intentionally skipped.
     */
    SKIPPED,

    /**
     * Specified entrypoint class or target method could not be found.
     */
    NOT_FOUND
}
