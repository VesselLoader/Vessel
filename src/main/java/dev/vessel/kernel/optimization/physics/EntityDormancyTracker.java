package dev.vessel.kernel.optimization.physics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lithium and Purpur-inspired dormancy and idle entity optimization manager.
 * <p>
 * In production servers, hundreds or thousands of entities (livestock in 1x1 breeding pens,
 * villagers, or idle monsters in unloaded corners) continuously consume 60-80% of server
 * tick time re-evaluating physics collisions, ray-tracing, and pathfinding while stationary.
 * </p>
 * <p>
 * This tracker observes entity position deltas over consecutive ticks. When an entity remains
 * within an epsilon tolerance for a configured idle threshold (default 20 ticks = 1 second),
 * it transitions to {@code DORMANT} state, signaling the physics engine to bypass full collision
 * checks. Dormant entities are instantly awoken when receiving player interactions, nearby block changes,
 * or external impulse vectors.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class EntityDormancyTracker {

    public static final int DEFAULT_IDLE_TICKS_THRESHOLD = 20;
    public static final double DEFAULT_MOVEMENT_TOLERANCE = 0.001; // epsilon delta
    public static final double DEFAULT_MOVEMENT_TOLERANCE_SQ = DEFAULT_MOVEMENT_TOLERANCE * DEFAULT_MOVEMENT_TOLERANCE;

    private final Map<Integer, EntityDormancyEntry> entries;
    private final AtomicInteger dormantCount;
    private volatile int idleTicksThreshold;
    private volatile double movementToleranceSq;
    private final AtomicLong currentTickCounter;

    /**
     * Constructs an EntityDormancyTracker with default thresholds.
     */
    public EntityDormancyTracker() {
        this(DEFAULT_IDLE_TICKS_THRESHOLD, DEFAULT_MOVEMENT_TOLERANCE);
    }

    /**
     * Constructs an EntityDormancyTracker with configurable thresholds.
     *
     * @param idleTicksThreshold ticks of stillness before entering dormancy
     * @param movementTolerance  maximum movement distance per tick considered stationary
     */
    public EntityDormancyTracker(int idleTicksThreshold, double movementTolerance) {
        if (idleTicksThreshold < 1) {
            throw new IllegalArgumentException("idleTicksThreshold must be >= 1: " + idleTicksThreshold);
        }
        if (movementTolerance < 0.0) {
            throw new IllegalArgumentException("movementTolerance must be non-negative: " + movementTolerance);
        }

        this.entries = new ConcurrentHashMap<>();
        this.dormantCount = new AtomicInteger(0);
        this.idleTicksThreshold = idleTicksThreshold;
        this.movementToleranceSq = movementTolerance * movementTolerance;
        this.currentTickCounter = new AtomicLong(0L);
    }

    /**
     * Registers a newly spawned or loaded entity for dormancy tracking.
     *
     * @param entityId unique entity identifier
     * @param posX     initial X
     * @param posY     initial Y
     * @param posZ     initial Z
     */
    public void registerEntity(int entityId, double posX, double posY, double posZ) {
        entries.compute(entityId, (k, existing) -> {
            if (existing != null) {
                if (existing.isDormant()) {
                    dormantCount.decrementAndGet();
                }
                existing.setPosition(posX, posY, posZ);
                existing.markAwake(currentTickCounter.get());
                return existing;
            }
            return new EntityDormancyEntry(entityId, posX, posY, posZ);
        });
    }

    /**
     * Unregisters an entity when despawned, killed, or moved to another dimension.
     *
     * @param entityId entity identifier
     * @return true if entity was tracked and removed
     */
    public boolean unregisterEntity(int entityId) {
        EntityDormancyEntry removed = entries.remove(entityId);
        if (removed != null) {
            if (removed.isDormant()) {
                dormantCount.decrementAndGet();
            }
            return true;
        }
        return false;
    }

    /**
     * Records the position of an entity during the current tick and updates dormancy status.
     *
     * @param entityId entity identifier
     * @param posX     current X
     * @param posY     current Y
     * @param posZ     current Z
     * @return true if the entity is currently dormant, false if active
     */
    public boolean recordTickPosition(int entityId, double posX, double posY, double posZ) {
        EntityDormancyEntry entry = entries.get(entityId);
        if (entry == null) {
            registerEntity(entityId, posX, posY, posZ);
            return false;
        }

        double dx = posX - entry.getLastPosX();
        double dy = posY - entry.getLastPosY();
        double dz = posZ - entry.getLastPosZ();
        double distSq = dx * dx + dy * dy + dz * dz;

        entry.setPosition(posX, posY, posZ);

        if (distSq <= movementToleranceSq) {
            entry.incrementStationaryTicks();
            if (entry.getStationaryTicks() >= idleTicksThreshold && !entry.isDormant()) {
                entry.setDormant(true);
                dormantCount.incrementAndGet();
            }
        } else {
            if (entry.isDormant()) {
                entry.setDormant(false);
                dormantCount.decrementAndGet();
            }
            entry.resetStationaryTicks();
        }

        return entry.isDormant();
    }

    /**
     * Immediately awakens an entity (e.g. upon taking damage, receiving player interaction, or impulse).
     *
     * @param entityId entity identifier
     * @return true if entity was found and awoken from dormancy, false otherwise
     */
    public boolean wake(int entityId) {
        EntityDormancyEntry entry = entries.get(entityId);
        if (entry != null && entry.isDormant()) {
            entry.markAwake(currentTickCounter.get());
            dormantCount.decrementAndGet();
            return true;
        }
        return false;
    }

    /**
     * Awakens all dormant entities within a spherical radius of a spatial disturbance
     * (e.g., explosion, block break, piston movement).
     *
     * @param originX disturbance center X
     * @param originY disturbance center Y
     * @param originZ disturbance center Z
     * @param radius  radius of disturbance
     * @return count of dormant entities awoken
     */
    public int wakeRadius(double originX, double originY, double originZ, double radius) {
        if (radius <= 0.0) {
            return 0;
        }
        double radiusSq = radius * radius;
        long currentTick = currentTickCounter.get();
        int awokenCount = 0;

        for (EntityDormancyEntry entry : entries.values()) {
            if (entry.isDormant()) {
                double dx = entry.getLastPosX() - originX;
                double dy = entry.getLastPosY() - originY;
                double dz = entry.getLastPosZ() - originZ;
                if ((dx * dx + dy * dy + dz * dz) <= radiusSq) {
                    entry.markAwake(currentTick);
                    dormantCount.decrementAndGet();
                    awokenCount++;
                }
            }
        }
        return awokenCount;
    }

    /**
     * Awakens all tracked entities in the world.
     */
    public void wakeAll() {
        long currentTick = currentTickCounter.get();
        for (EntityDormancyEntry entry : entries.values()) {
            if (entry.isDormant()) {
                entry.markAwake(currentTick);
            }
        }
        dormantCount.set(0);
    }

    /**
     * Advances the world tick counter.
     *
     * @param tick current game tick
     */
    public void advanceTick(long tick) {
        currentTickCounter.set(tick);
    }

    /**
     * Checks whether the given entity is dormant.
     *
     * @param entityId entity identifier
     * @return true if dormant
     */
    public boolean isDormant(int entityId) {
        EntityDormancyEntry entry = entries.get(entityId);
        return entry != null && entry.isDormant();
    }

    /**
     * Returns the count of currently dormant entities.
     *
     * @return dormant entity count
     */
    public int getDormantCount() {
        return dormantCount.get();
    }

    /**
     * Returns the count of active (non-dormant) tracked entities.
     *
     * @return active entity count
     */
    public int getActiveCount() {
        return Math.max(0, entries.size() - dormantCount.get());
    }

    /**
     * Returns total number of registered entities.
     *
     * @return tracked entity count
     */
    public int getTrackedCount() {
        return entries.size();
    }

    public int getIdleTicksThreshold() {
        return idleTicksThreshold;
    }

    public void setIdleTicksThreshold(int idleTicksThreshold) {
        if (idleTicksThreshold < 1) {
            throw new IllegalArgumentException("idleTicksThreshold must be >= 1: " + idleTicksThreshold);
        }
        this.idleTicksThreshold = idleTicksThreshold;
    }

    public double getMovementTolerance() {
        return Math.sqrt(movementToleranceSq);
    }

    public void setMovementTolerance(double movementTolerance) {
        if (movementTolerance < 0.0) {
            throw new IllegalArgumentException("movementTolerance must be non-negative: " + movementTolerance);
        }
        this.movementToleranceSq = movementTolerance * movementTolerance;
    }

    /**
     * Clears all registered entities.
     */
    public void clear() {
        entries.clear();
        dormantCount.set(0);
    }
}
