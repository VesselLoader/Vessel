package dev.vessel.kernel.world.worldgen;

/**
 * Access and fallback modes for the read-only phantom chunk view.
 * <p>
 * Directly resolves ERR-34 and WGN-0251..0500.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum PhantomChunkAccessMode {

    /**
     * Fallback to synthetic air blocks for all queries in ungenerated chunks.
     */
    SYNTHETIC_AIR,

    /**
     * Fallback to synthetic solid stone terrain below sea level and air above.
     */
    SYNTHETIC_TERRAIN,

    /**
     * Fallback to the void / empty world boundaries.
     */
    SYNTHETIC_VOID,

    /**
     * Queries delegate to loaded chunk memory if already present, otherwise fall back to synthetic terrain.
     */
    DELEGATE_IF_LOADED
}
