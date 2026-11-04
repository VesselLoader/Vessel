package dev.vessel.kernel.optimization.entity;

/**
 * Dynamic AI tick throttling engine inspired by Purpur and Canvas entity optimizations.
 * Adjusts brain ticking frequency based on entity proximity to the nearest active player,
 * preventing server tick rate degradation in crowded mob farms while preserving responsive
 * behavior for nearby interactions.
 * <p>
 * Employs bitwise stride masks for powers-of-two throttling intervals to eliminate CPU division
 * instructions across thousands of ticking entities.
 * </p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class EntityBrainTickThrottle {

    private final double closeThresholdSq;
    private final double mediumThresholdSq;
    private final double farThresholdSq;

    /**
     * Constructs an EntityBrainTickThrottle with customizable radial distance thresholds (in blocks).
     *
     * @param closeRadius distance within which mobs tick every tick (e.g. 16.0)
     * @param mediumRadius distance within which mobs tick every 2 ticks (e.g. 32.0)
     * @param farRadius distance within which mobs tick every 4 ticks (e.g. 64.0)
     */
    public EntityBrainTickThrottle(double closeRadius, double mediumRadius, double farRadius) {
        this.closeThresholdSq = closeRadius * closeRadius;
        this.mediumThresholdSq = mediumRadius * mediumRadius;
        this.farThresholdSq = farRadius * farRadius;
    }

    public EntityBrainTickThrottle() {
        this(16.0, 32.0, 64.0);
    }

    /**
     * Computes the tick interval (stride) for an entity at the specified distance squared.
     *
     * @param nearestPlayerDistanceSq distance squared to closest player
     * @return tick interval (1, 2, 4, or 10)
     */
    public int computeTickInterval(double nearestPlayerDistanceSq) {
        if (nearestPlayerDistanceSq <= closeThresholdSq) {
            return 1; // Full fidelity: tick every tick
        } else if (nearestPlayerDistanceSq <= mediumThresholdSq) {
            return 2; // Tick every 2nd tick
        } else if (nearestPlayerDistanceSq <= farThresholdSq) {
            return 4; // Tick every 4th tick
        } else {
            return 10; // Distant mob: tick once every 10 ticks
        }
    }

    /**
     * Determines whether an entity brain should execute an AI step on the current game tick.
     *
     * @param entityId unique entity ID (used to stagger ticks and prevent phase lock)
     * @param currentTick current world game tick
     * @param nearestPlayerDistanceSq distance squared to closest player
     * @return true if entity AI should execute this tick
     */
    public boolean shouldTick(int entityId, long currentTick, double nearestPlayerDistanceSq) {
        int interval = computeTickInterval(nearestPlayerDistanceSq);
        if (interval == 1) {
            return true;
        }
        long offsetTick = currentTick + (entityId & 0x7FFF);
        if (interval == 2) {
            return (offsetTick & 1L) == 0L;
        } else if (interval == 4) {
            return (offsetTick & 3L) == 0L;
        }
        return offsetTick % interval == 0L;
    }
}
