package dev.vessel.kernel.migration;

/**
 * Capture trigger mode defining the priority and criteria for chunk snapshotting.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum ChunkSnapshotCaptureMode {

    /** Periodic background autosave pass. */
    FULL_AUTOSAVE,

    /** Incremental capture capturing only modified sections. */
    DIRTY_ONLY,

    /** High-priority flush during server shutdown or world unmount. */
    SHUTDOWN_FLUSH,

    /** Explicit programmatic snapshot request. */
    ON_DEMAND
}
