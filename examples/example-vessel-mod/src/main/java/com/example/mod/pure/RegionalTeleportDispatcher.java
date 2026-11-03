package com.example.mod.pure;

import dev.vessel.api.actor.FoliaRegionExecutionBridge;

import java.util.Objects;

/**
 * Demonstrates thread-safe, regional actor concurrency across Paper, Bukkit, and Folia
 * using VESSEL Super-API.
 *
 * <p>On Folia servers, tasks are dispatched to the specific regional thread owning the chunk coordinates.
 * On Paper/Spigot/Bukkit/Forge servers, tasks execute transparently on the main server thread.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 * @since 1.0.0
 */
public final class RegionalTeleportDispatcher {

    private static final String MOD_HANDLE = "example_vessel_mod";
    private final FoliaRegionExecutionBridge executionBridge;

    /**
     * Constructs a new regional teleport dispatcher.
     */
    public RegionalTeleportDispatcher() {
        this.executionBridge = FoliaRegionExecutionBridge.getInstance();
    }

    /**
     * Dispatches a regional mutation at specific world chunk coordinates asynchronously.
     *
     * @param worldHandle target world handle
     * @param chunkX      chunk X coordinate
     * @param chunkZ      chunk Z coordinate
     * @param action      workload to execute safely on the owning regional strand
     */
    public void dispatchRegionalWork(Object worldHandle, int chunkX, int chunkZ, Runnable action) {
        Objects.requireNonNull(worldHandle, "worldHandle cannot be null");
        Objects.requireNonNull(action, "action cannot be null");

        executionBridge.executeInRegion(MOD_HANDLE, worldHandle, chunkX, chunkZ, action);
    }

    /**
     * Dispatches a global server task (such as cross-region sync or broadcast).
     *
     * @param action workload to execute on the global tick thread
     */
    public void dispatchGlobalTask(Runnable action) {
        Objects.requireNonNull(action, "action cannot be null");
        executionBridge.scheduleGlobalTask(MOD_HANDLE, action);
    }

    /**
     * Schedules a delayed regional task after a specified tick delay.
     *
     * @param worldHandle target world handle
     * @param chunkX      chunk X coordinate
     * @param chunkZ      chunk Z coordinate
     * @param delayTicks  delay in ticks (20 ticks = 1 second)
     * @param action      delayed task workload
     */
    public void dispatchDelayedRegionalTask(Object worldHandle, int chunkX, int chunkZ, long delayTicks, Runnable action) {
        Objects.requireNonNull(worldHandle, "worldHandle cannot be null");
        Objects.requireNonNull(action, "action cannot be null");

        executionBridge.executeInRegionDelayed(MOD_HANDLE, worldHandle, chunkX, chunkZ, action, delayTicks);
    }
}
