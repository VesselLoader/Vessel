package dev.vessel.kernel.world.worldgen;

/**
 * Status of a world generation permit request evaluated by the throttler.
 * <p>
 * Directly resolves ERR-33 and WGN-0001..0250.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum CascadingPermitStatus {

    /**
     * Generation permit granted; caller thread may proceed with chunk generation.
     */
    ACQUIRED,

    /**
     * Permit rejected because cascading recursion depth exceeds configured limit.
     */
    REJECTED_DEPTH_EXCEEDED,

    /**
     * Permit rejected because an active circular dependency cycle was detected across threads.
     */
    REJECTED_CYCLE_DETECTED,

    /**
     * Permit rejected because the maximum concurrent generation capacity is saturated.
     */
    REJECTED_CAPACITY_EXCEEDED,

    /**
     * Permit was previously acquired and has now been safely released.
     */
    RELEASED
}
