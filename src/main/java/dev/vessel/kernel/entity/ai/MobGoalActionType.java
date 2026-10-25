package dev.vessel.kernel.entity.ai;

/**
 * Classifies the behavioral domain controlled by an entity AI goal.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public enum MobGoalActionType {

    /**
     * Controls movement and navigation pathfinding.
     */
    MOVE,

    /**
     * Controls head and gaze direction.
     */
    LOOK,

    /**
     * Controls combat or pursuit target selection.
     */
    TARGET,

    /**
     * Controls jumping or vertical propulsion.
     */
    JUMP,

    /**
     * Controls attack actions and cooldowns.
     */
    ATTACK,

    /**
     * Controls swimming and water navigation.
     */
    SWIM,

    /**
     * Controls player or block interactions (e.g. trading, opening doors).
     */
    INTERACT,

    /**
     * Modded or plugin custom behavior.
     */
    CUSTOM
}
