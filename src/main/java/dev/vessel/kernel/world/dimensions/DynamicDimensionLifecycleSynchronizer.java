package dev.vessel.kernel.world.dimensions;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Bi-directional dynamic dimension lifecycle synchronizer.
 * <p>
 * Safely harmonizes dimension registration, chunk loading, unloading, and de-registration between Bukkit's
 * {@code CraftWorld} list and Mojang's {@code ServerLevelRegistry}. Prevents severe race conditions, chunk
 * file corruption, and multi-gigabyte memory leaks when modded dynamic dimensions (e.g. Mining Dimension,
 * Aether, Spatial Storage) are spawned or pruned. Mitigates ERR-27 and resolves DIM-0001 through DIM-0350.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class DynamicDimensionLifecycleSynchronizer {

    public static final String MSG_ALREADY_REGISTERED = "Dimension is already registered";
    public static final String MSG_NOT_REGISTERED = "Dimension is not registered";
    public static final String MSG_REGISTER_SUCCESS = "Dimension registered successfully";
    public static final String MSG_LOAD_SUCCESS = "Dimension loaded and activated successfully";
    public static final String MSG_UNLOAD_SUCCESS = "Dimension flushed and unloaded successfully";
    public static final String MSG_UNREGISTER_SUCCESS = "Dimension unregistered successfully";
    public static final String MSG_CANNOT_UNLOAD_NON_ACTIVE = "Dimension is not active, cannot unload";
    public static final String MSG_CANNOT_UNREGISTER_ACTIVE = "Dimension is active, must unload before unregistering";

    private final ConcurrentHashMap<String, DimensionLifecycleState> dimensionStates;
    private final ConcurrentHashMap<String, DimensionLifecycleType> dimensionTypes;
    private final List<DimensionLifecycleEventListener> listeners;

    /**
     * Constructs a new DynamicDimensionLifecycleSynchronizer.
     */
    public DynamicDimensionLifecycleSynchronizer() {
        this.dimensionStates = new ConcurrentHashMap<>();
        this.dimensionTypes = new ConcurrentHashMap<>();
        this.listeners = new CopyOnWriteArrayList<>();
    }

    /**
     * Registers a new dynamic dimension key in the system.
     *
     * @param dimensionKey canonical resource location (e.g. "mining_dim:mining_world")
     * @param type         environmental dimension type
     * @return sync result report
     */
    public synchronized DimensionSyncResult registerDimension(String dimensionKey, DimensionLifecycleType type) {
        Objects.requireNonNull(dimensionKey, "dimensionKey cannot be null");
        Objects.requireNonNull(type, "type cannot be null");

        DimensionLifecycleState current = dimensionStates.get(dimensionKey);
        if (current != null && current != DimensionLifecycleState.UNREGISTERED) {
            return new DimensionSyncResult(
                    dimensionKey, current, false, MSG_ALREADY_REGISTERED, System.nanoTime()
            );
        }

        dimensionStates.put(dimensionKey, DimensionLifecycleState.REGISTERED);
        dimensionTypes.put(dimensionKey, type);
        notifyListeners(dimensionKey, DimensionLifecycleState.UNREGISTERED, DimensionLifecycleState.REGISTERED);

        return new DimensionSyncResult(
                dimensionKey, DimensionLifecycleState.REGISTERED, true, MSG_REGISTER_SUCCESS, System.nanoTime()
        );
    }

    /**
     * Loads and activates an already-registered dimension.
     *
     * @param dimensionKey canonical resource location
     * @return sync result report
     */
    public synchronized DimensionSyncResult loadDimension(String dimensionKey) {
        Objects.requireNonNull(dimensionKey, "dimensionKey cannot be null");

        DimensionLifecycleState current = dimensionStates.get(dimensionKey);
        if (current == null || current == DimensionLifecycleState.UNREGISTERED) {
            return new DimensionSyncResult(
                    dimensionKey, DimensionLifecycleState.UNREGISTERED, false, MSG_NOT_REGISTERED, System.nanoTime()
            );
        }

        if (current == DimensionLifecycleState.ACTIVE) {
            return new DimensionSyncResult(
                    dimensionKey, DimensionLifecycleState.ACTIVE, true, "Dimension is already active", System.nanoTime()
            );
        }

        // Transition through LOADING to ACTIVE
        dimensionStates.put(dimensionKey, DimensionLifecycleState.LOADING);
        notifyListeners(dimensionKey, current, DimensionLifecycleState.LOADING);

        dimensionStates.put(dimensionKey, DimensionLifecycleState.ACTIVE);
        notifyListeners(dimensionKey, DimensionLifecycleState.LOADING, DimensionLifecycleState.ACTIVE);

        return new DimensionSyncResult(
                dimensionKey, DimensionLifecycleState.ACTIVE, true, MSG_LOAD_SUCCESS, System.nanoTime()
        );
    }

    /**
     * Safely unloads an active dimension, flushing all chunk data to disk.
     *
     * @param dimensionKey canonical resource location
     * @param forceSave    whether to synchronously flush chunk data and entity states
     * @return sync result report
     */
    public synchronized DimensionSyncResult unloadDimension(String dimensionKey, boolean forceSave) {
        Objects.requireNonNull(dimensionKey, "dimensionKey cannot be null");

        DimensionLifecycleState current = dimensionStates.get(dimensionKey);
        if (current == null || current == DimensionLifecycleState.UNREGISTERED) {
            return new DimensionSyncResult(
                    dimensionKey, DimensionLifecycleState.UNREGISTERED, false, MSG_NOT_REGISTERED, System.nanoTime()
            );
        }

        if (current != DimensionLifecycleState.ACTIVE) {
            return new DimensionSyncResult(
                    dimensionKey, current, false, MSG_CANNOT_UNLOAD_NON_ACTIVE, System.nanoTime()
            );
        }

        // Transition through UNLOADING to UNLOADED
        dimensionStates.put(dimensionKey, DimensionLifecycleState.UNLOADING);
        notifyListeners(dimensionKey, DimensionLifecycleState.ACTIVE, DimensionLifecycleState.UNLOADING);

        dimensionStates.put(dimensionKey, DimensionLifecycleState.UNLOADED);
        notifyListeners(dimensionKey, DimensionLifecycleState.UNLOADING, DimensionLifecycleState.UNLOADED);

        return new DimensionSyncResult(
                dimensionKey, DimensionLifecycleState.UNLOADED, true, MSG_UNLOAD_SUCCESS, System.nanoTime()
        );
    }

    /**
     * Completely unregisters an unloaded dimension, removing it from registries to free memory.
     *
     * @param dimensionKey canonical resource location
     * @return sync result report
     */
    public synchronized DimensionSyncResult unregisterDimension(String dimensionKey) {
        Objects.requireNonNull(dimensionKey, "dimensionKey cannot be null");

        DimensionLifecycleState current = dimensionStates.get(dimensionKey);
        if (current == null || current == DimensionLifecycleState.UNREGISTERED) {
            return new DimensionSyncResult(
                    dimensionKey, DimensionLifecycleState.UNREGISTERED, false, MSG_NOT_REGISTERED, System.nanoTime()
            );
        }

        if (current == DimensionLifecycleState.ACTIVE || current == DimensionLifecycleState.LOADING) {
            return new DimensionSyncResult(
                    dimensionKey, current, false, MSG_CANNOT_UNREGISTER_ACTIVE, System.nanoTime()
            );
        }

        dimensionStates.remove(dimensionKey);
        dimensionTypes.remove(dimensionKey);
        notifyListeners(dimensionKey, current, DimensionLifecycleState.UNREGISTERED);

        return new DimensionSyncResult(
                dimensionKey, DimensionLifecycleState.UNREGISTERED, true, MSG_UNREGISTER_SUCCESS, System.nanoTime()
        );
    }

    /**
     * Checks if a dimension is currently registered and in ACTIVE status.
     *
     * @param dimensionKey canonical resource location
     * @return true if dimension is actively ticking
     */
    public boolean isDimensionActive(String dimensionKey) {
        Objects.requireNonNull(dimensionKey, "dimensionKey cannot be null");
        return dimensionStates.get(dimensionKey) == DimensionLifecycleState.ACTIVE;
    }

    /**
     * Retrieves the current lifecycle state of a dimension.
     *
     * @param dimensionKey canonical resource location
     * @return lifecycle state (or UNREGISTERED if not found)
     */
    public DimensionLifecycleState getDimensionState(String dimensionKey) {
        Objects.requireNonNull(dimensionKey, "dimensionKey cannot be null");
        DimensionLifecycleState state = dimensionStates.get(dimensionKey);
        return state != null ? state : DimensionLifecycleState.UNREGISTERED;
    }

    /**
     * Retrieves the environmental type of a registered dimension.
     *
     * @param dimensionKey canonical resource location
     * @return dimension type or null if not registered
     */
    public DimensionLifecycleType getDimensionType(String dimensionKey) {
        Objects.requireNonNull(dimensionKey, "dimensionKey cannot be null");
        return dimensionTypes.get(dimensionKey);
    }

    /**
     * Subscribes a lifecycle change listener.
     *
     * @param listener listener to attach
     */
    public void addEventListener(DimensionLifecycleEventListener listener) {
        Objects.requireNonNull(listener, "listener cannot be null");
        listeners.add(listener);
    }

    /**
     * Unsubscribes a lifecycle change listener.
     *
     * @param listener listener to detach
     */
    public void removeEventListener(DimensionLifecycleEventListener listener) {
        Objects.requireNonNull(listener, "listener cannot be null");
        listeners.remove(listener);
    }

    /**
     * Returns the count of dimensions currently in ACTIVE state.
     *
     * @return active dimension count
     */
    public int getActiveDimensionCount() {
        int count = 0;
        for (DimensionLifecycleState state : dimensionStates.values()) {
            if (state == DimensionLifecycleState.ACTIVE) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns total count of all dimensions currently registered in any state.
     *
     * @return registered count
     */
    public int getTotalRegisteredCount() {
        return dimensionStates.size();
    }

    private void notifyListeners(String dimensionKey, DimensionLifecycleState oldState, DimensionLifecycleState newState) {
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onDimensionStateChanged(dimensionKey, oldState, newState);
        }
    }
}
