package dev.vessel.kernel.entity.ai;

/**
 * Enumeration of originating subsystems that register entity AI goals.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public enum MobGoalSource {

    /**
     * Minecraft Vanilla native entity AI goal.
     */
    VANILLA,

    /**
     * Paper/Bukkit plugin registering via Paper MobGoals API.
     */
    PAPER_BUKKIT,

    /**
     * Forge/NeoForge mod registering via GoalSelector or Brain/Behavior.
     */
    FORGE_NEOFORGE,

    /**
     * Fabric mod registering via Fabric entity events.
     */
    FABRIC,

    /**
     * Internal VESSEL engine optimization or AI governor.
     */
    VESSEL_CORE
}
