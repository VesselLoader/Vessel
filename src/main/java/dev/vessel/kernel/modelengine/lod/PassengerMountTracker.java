package dev.vessel.kernel.modelengine.lod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages the hierarchical passenger mounting structure where model bone Display entities
 * and Interaction hitboxes are mounted as passengers on the root driver mob.
 * <p>
 * By utilizing the vanilla client's native vehicle-passenger interpolation engine,
 * the server does NOT need to transmit per-tick position/teleport packets for each bone
 * as the entity moves across the world, achieving over 90% network packet reduction.
 * </p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class PassengerMountTracker {

    private final int vehicleEntityId;
    private final List<Integer> passengerEntityIds = new CopyOnWriteArrayList<>();

    private final AtomicLong savedTeleportPackets = new AtomicLong(0);
    private final AtomicLong totalMovementTicks = new AtomicLong(0);

    /**
     * Constructs a PassengerMountTracker for a given root vehicle entity.
     *
     * @param vehicleEntityId root driver mob entity ID
     */
    public PassengerMountTracker(int vehicleEntityId) {
        this.vehicleEntityId = vehicleEntityId;
    }

    /**
     * Mounts a passenger entity (bone display entity or interaction hitbox) onto this vehicle.
     *
     * @param passengerEntityId passenger entity ID
     */
    public void addPassenger(int passengerEntityId) {
        if (!passengerEntityIds.contains(passengerEntityId)) {
            passengerEntityIds.add(passengerEntityId);
        }
    }

    /**
     * Unmounts a passenger entity.
     *
     * @param passengerEntityId passenger entity ID
     */
    public void removePassenger(int passengerEntityId) {
        passengerEntityIds.remove(Integer.valueOf(passengerEntityId));
    }

    /**
     * Records a mob movement tick, updating saved network packet statistics.
     */
    public void recordMovementTick() {
        totalMovementTicks.incrementAndGet();
        savedTeleportPackets.addAndGet(passengerEntityIds.size());
    }

    /**
     * Calculates the estimated network packet reduction percentage achieved by passenger mounting.
     *
     * @return packet reduction fraction (e.g. 0.95 for 95% reduction)
     */
    public double calculatePacketReductionRatio() {
        int passengers = passengerEntityIds.size();
        if (passengers == 0) {
            return 0.0;
        }
        // Without mount: (1 driver packet + N passenger packets) = N + 1 packets per move
        // With mount: 1 driver packet = 1 packet per move
        // Packets saved = N / (N + 1)
        return (double) passengers / (passengers + 1.0);
    }

    public int getVehicleEntityId() {
        return vehicleEntityId;
    }

    public List<Integer> getPassengerEntityIds() {
        return Collections.unmodifiableList(new ArrayList<>(passengerEntityIds));
    }

    public int getPassengerCount() {
        return passengerEntityIds.size();
    }

    public long getSavedTeleportPackets() {
        return savedTeleportPackets.get();
    }

    public long getTotalMovementTicks() {
        return totalMovementTicks.get();
    }
}
