package dev.vessel.kernel.optimization.supermod;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Governs Valkyrien Skies 2, Eureka! Ships!, Clockwork, and Create Aeronautics voxel ship physics.
 * Places stationary or micro-vibrating ships (&lt; {@code 0.002 m/tick} for 20 ticks) into Physics
 * Sleep Mode, wakes ships on block interaction, budgets VoxelShape AABB collision sweeps, and
 * coalesces 60 Hz physics thread transforms to the 20 TPS Folia region thread.
 *
 * @version 1.0.0
 */
public final class ValkyrienPhysicsAndVoxelShipCollisionGovernor {

    private static final double MICRO_VIBRATION_VELOCITY_SQ_THRESHOLD = 0.002 * 0.002;
    private static final int SLEEP_QUIET_TICKS_REQUIRED = 20;

    private final Set<Long> sleepingShips = ConcurrentHashMap.newKeySet();
    private final Map<Long, double[]> coalescedShipTransforms = new ConcurrentHashMap<>();

    /**
     * Evaluates whether a physics ship should enter or remain in Physics Sleep Mode due to
     * near-zero linear and angular velocity over {@code quietTicks}.
     *
     * @param shipId            the unique physics ship identifier
     * @param linearVelocitySq  squared linear velocity in {@code (m/tick)^2}
     * @param angularVelocitySq squared angular velocity in {@code (rad/tick)^2}
     * @param quietTicks        number of consecutive ticks below the micro-vibration threshold
     * @return {@code true} if the ship is placed into Physics Sleep Mode (skipping VoxelShape
     *         collision sweeps)
     */
    public boolean evaluateShipMicroVibrationSleep(
            long shipId,
            double linearVelocitySq,
            double angularVelocitySq,
            int quietTicks) {
        if (linearVelocitySq < MICRO_VIBRATION_VELOCITY_SQ_THRESHOLD
                && angularVelocitySq < MICRO_VIBRATION_VELOCITY_SQ_THRESHOLD
                && quietTicks >= SLEEP_QUIET_TICKS_REQUIRED) {
            sleepingShips.add(shipId);
            return true;
        }
        sleepingShips.remove(shipId);
        return false;
    }

    /**
     * Immediately wakes a sleeping physics ship when a block on or adjacent to the ship is
     * interacted with.
     *
     * @param shipId             the unique physics ship identifier
     * @param interactedBlockPos the packed 64-bit interacted block position
     */
    public void wakePhysicsShipOnInteraction(long shipId, long interactedBlockPos) {
        sleepingShips.remove(shipId);
    }

    /**
     * Budgets the number of VoxelShape AABB collision sweeps a ship may perform during the current
     * tick, returning {@code 0} if the ship is currently in Physics Sleep Mode.
     *
     * @param shipId               the unique physics ship identifier
     * @param requestedVoxelSweeps the number of voxel collision sweeps requested
     * @param maxSweepsPerTick     the per-ship per-tick sweep cap
     * @return the allowed number of voxel collision sweeps
     */
    public int budgetShipVoxelCollisionSweeps(
            long shipId,
            int requestedVoxelSweeps,
            int maxSweepsPerTick) {
        if (sleepingShips.contains(shipId) || requestedVoxelSweeps <= 0 || maxSweepsPerTick <= 0) {
            return 0;
        }
        return Math.min(requestedVoxelSweeps, maxSweepsPerTick);
    }

    /**
     * Coalesces high-frequency (60 Hz) physics thread ship pose updates into a latest-value buffer
     * for 20 TPS consumption by the owning Folia region thread.
     *
     * @param shipId the unique physics ship identifier
     * @param x      the ship world X coordinate
     * @param y      the ship world Y coordinate
     * @param z      the ship world Z coordinate
     * @param yaw    the ship yaw angle
     * @param pitch  the ship pitch angle
     * @param roll   the ship roll angle
     * @return {@code true} if the transform is finite and was coalesced
     */
    public boolean coalesceShipTransformToRegionThread(
            long shipId,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            float roll) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Float.isFinite(yaw) || !Float.isFinite(pitch) || !Float.isFinite(roll)) {
            return false;
        }
        coalescedShipTransforms.put(shipId, new double[]{x, y, z, yaw, pitch, roll});
        return true;
    }
}
