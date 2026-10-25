package dev.vessel.kernel.modelengine.lod;

/**
 * Level-Of-Detail (LOD) tiers for skeletal bone animation calculations and packet broadcasts.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum ModelAnimationLODTier {

    /**
     * Near proximity (&lt; 16m): Full 20 ticks/second bone animation calculation and synchronization.
     */
    NEAR(1, 20),

    /**
     * Mid proximity (16m - 32m): Throttled 5 ticks/second bone calculation (every 4 ticks).
     */
    MID(4, 5),

    /**
     * Far proximity (&gt; 32m): Bone calculations completely frozen. Only root entity moves.
     */
    FAR(0, 0),

    /**
     * Unobserved or dormant (no active players nearby): Zero CPU overhead.
     */
    DORMANT(0, 0);

    private final int updateIntervalTicks;
    private final int updatesPerSecond;

    ModelAnimationLODTier(int updateIntervalTicks, int updatesPerSecond) {
        this.updateIntervalTicks = updateIntervalTicks;
        this.updatesPerSecond = updatesPerSecond;
    }

    /**
     * Checks if this tier should execute an animation calculation on the current tick.
     * Staggers mid-tier updates using entity ID to eliminate frame-time spikes across mobs.
     *
     * @param currentTick server tick count
     * @param entityId    unique entity identifier
     * @return true if bone animation should be calculated and synced
     */
    public boolean shouldUpdateAnimation(long currentTick, int entityId) {
        if (this == NEAR) {
            return true;
        }
        if (this == MID) {
            return ((currentTick + entityId) % 4) == 0;
        }
        return false;
    }

    public int getUpdateIntervalTicks() {
        return updateIntervalTicks;
    }

    public int getUpdatesPerSecond() {
        return updatesPerSecond;
    }
}
