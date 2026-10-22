package dev.vessel.kernel.optimization.physics;

/**
 * Spatial and temporal dormancy state container for a single tracked entity.
 * <p>
 * Tracks continuous stationary tick counts and coordinates to determine
 * when entity collision and physics ticks can be safely suspended.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class EntityDormancyEntry {

    private final int entityId;
    private double lastPosX;
    private double lastPosY;
    private double lastPosZ;
    private int stationaryTicks;
    private boolean dormant;
    private long lastWakeTick;

    /**
     * Constructs a new EntityDormancyEntry.
     *
     * @param entityId entity unique identifier
     * @param initialX initial X coordinate
     * @param initialY initial Y coordinate
     * @param initialZ initial Z coordinate
     */
    public EntityDormancyEntry(int entityId, double initialX, double initialY, double initialZ) {
        this.entityId = entityId;
        this.lastPosX = initialX;
        this.lastPosY = initialY;
        this.lastPosZ = initialZ;
        this.stationaryTicks = 0;
        this.dormant = false;
        this.lastWakeTick = 0L;
    }

    public int getEntityId() {
        return entityId;
    }

    public double getLastPosX() {
        return lastPosX;
    }

    public double getLastPosY() {
        return lastPosY;
    }

    public double getLastPosZ() {
        return lastPosZ;
    }

    public int getStationaryTicks() {
        return stationaryTicks;
    }

    public boolean isDormant() {
        return dormant;
    }

    public long getLastWakeTick() {
        return lastWakeTick;
    }

    public void setPosition(double posX, double posY, double posZ) {
        this.lastPosX = posX;
        this.lastPosY = posY;
        this.lastPosZ = posZ;
    }

    public void incrementStationaryTicks() {
        this.stationaryTicks++;
    }

    public void resetStationaryTicks() {
        this.stationaryTicks = 0;
    }

    public void setDormant(boolean dormant) {
        this.dormant = dormant;
    }

    public void markAwake(long currentTick) {
        this.dormant = false;
        this.stationaryTicks = 0;
        this.lastWakeTick = currentTick;
    }
}
