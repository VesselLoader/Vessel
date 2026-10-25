package dev.vessel.kernel.entity.ai;

/**
 * Execution lifecycle states of an entity AI goal.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public enum MobGoalExecutionState {

    /**
     * Goal is registered but not actively ticking.
     */
    INACTIVE,

    /**
     * Goal satisfies starting conditions and is actively executing.
     */
    RUNNING,

    /**
     * Goal is temporarily suspended by a higher priority conflicting goal.
     */
    PAUSED
}
