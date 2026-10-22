package dev.vessel.kernel.optimization.entity;

/**
 * Geometric radial Level-Of-Detail (LOD) activation tiers for mob AI brain and pathfinding ticks.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum EntityActivationTier {
    ACTIVE(1, 16.0),
    NEAR(4, 32.0),
    FAR(16, 64.0),
    DORMANT(Integer.MAX_VALUE, Double.MAX_VALUE);

    private final int tickInterval;
    private final double maxDistance;

    EntityActivationTier(int tickInterval, double maxDistance) {
        this.tickInterval = tickInterval;
        this.maxDistance = maxDistance;
    }

    public int tickInterval() {
        return tickInterval;
    }

    public double maxDistance() {
        return maxDistance;
    }

    /**
     * Determines whether an entity in this tier should execute its brain tick on current tick.
     * Staggers executions across entities using the entity ID as phase offset to prevent micro-stutter spikes.
     *
     * @param currentTick current game tick
     * @param entityId entity identifier used as phase offset
     * @return true if entity should tick
     */
    public boolean shouldTick(long currentTick, int entityId) {
        if (this == DORMANT) {
            return false;
        }
        if (tickInterval == 1) {
            return true;
        }
        return ((currentTick + entityId) % tickInterval) == 0;
    }
}
