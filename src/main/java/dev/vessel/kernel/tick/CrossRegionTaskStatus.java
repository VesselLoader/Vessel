package dev.vessel.kernel.tick;

/**
 * Execution status lifecycle states for cross-region asynchronous actor messages.
 * <p>
 * Directly resolves ERR-31, ERR-32, and FOL-041..060.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum CrossRegionTaskStatus {

    /**
     * Enqueued in mailbox, waiting to be consumed by the destination strand.
     */
    PENDING,

    /**
     * Currently executing inside the local tick loop of the destination strand.
     */
    EXECUTING,

    /**
     * Task completed successfully and reply value (if any) was resolved.
     */
    COMPLETED,

    /**
     * Task exceeded allowed execution window and was aborted to prevent cross-region deadlock.
     */
    TIMED_OUT,

    /**
     * Task threw an unhandled exception during execution.
     */
    FAILED,

    /**
     * Task was cancelled before or during strand dispatch.
     */
    CANCELLED
}
