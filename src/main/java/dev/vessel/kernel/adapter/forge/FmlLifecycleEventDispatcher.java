package dev.vessel.kernel.adapter.forge;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * High-performance deterministic dispatcher for Forge Mod Loader (FML) lifecycle stages.
 * <p>
 * Sequences mod construction ({@code FMLConstructModEvent}), setup ({@code FMLCommonSetupEvent}),
 * and inter-mod communication enqueue ({@code InterModEnqueueEvent}).
 * Resolves: ERR-61 and ECO-001 through ECO-010.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class FmlLifecycleEventDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(FmlLifecycleEventDispatcher.class);

    public static final String ERROR_CODE_LIFECYCLE_TRANSITION = "ERR-61";
    public static final String ERROR_CODE_ECOSYSTEM_BASE = "ECO-00";

    private final Map<String, Map<FmlLifecyclePhase, List<FmlLifecycleListener>>> modListeners = new ConcurrentHashMap<>();
    private final Map<FmlLifecyclePhase, List<FmlLifecycleListener>> globalListeners = new ConcurrentHashMap<>();
    private final List<FmlLifecyclePhase> executedPhases = new CopyOnWriteArrayList<>();

    /**
     * Constructs a new FML lifecycle event dispatcher.
     */
    public FmlLifecycleEventDispatcher() {
    }

    /**
     * Registers a lifecycle listener for a specific mod and phase.
     *
     * @param modId    the mod identifier
     * @param phase    the lifecycle phase
     * @param listener the listener callback
     */
    public void registerListener(String modId, FmlLifecyclePhase phase, FmlLifecycleListener listener) {
        Objects.requireNonNull(modId, "modId must not be null");
        Objects.requireNonNull(phase, "phase must not be null");
        Objects.requireNonNull(listener, "listener must not be null");

        modListeners.computeIfAbsent(modId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(phase, k -> new CopyOnWriteArrayList<>())
                .add(listener);
    }

    /**
     * Registers a global lifecycle listener invoked for all mods during a given phase.
     *
     * @param phase    the lifecycle phase
     * @param listener the listener callback
     */
    public void registerGlobalListener(FmlLifecyclePhase phase, FmlLifecycleListener listener) {
        Objects.requireNonNull(phase, "phase must not be null");
        Objects.requireNonNull(listener, "listener must not be null");

        globalListeners.computeIfAbsent(phase, k -> new CopyOnWriteArrayList<>())
                .add(listener);
    }

    /**
     * Dispatches an event for a single mod at a specific lifecycle phase.
     *
     * @param phase          the target phase
     * @param modId          the recipient mod identifier
     * @param contextPayload optional contextual data
     */
    public void dispatch(FmlLifecyclePhase phase, String modId, Object contextPayload) {
        Objects.requireNonNull(phase, "phase must not be null");
        Objects.requireNonNull(modId, "modId must not be null");

        long timestamp = System.nanoTime();
        FmlLifecycleEvent event = new FmlLifecycleEvent(phase, modId, timestamp, contextPayload);

        // Execute mod-specific listeners
        Map<FmlLifecyclePhase, List<FmlLifecycleListener>> phaseMap = modListeners.get(modId);
        if (phaseMap != null) {
            List<FmlLifecycleListener> listeners = phaseMap.get(phase);
            if (listeners != null) {
                for (FmlLifecycleListener listener : listeners) {
                    invokeListenerSafely(listener, event);
                }
            }
        }

        // Execute global listeners
        List<FmlLifecycleListener> globals = globalListeners.get(phase);
        if (globals != null) {
            for (FmlLifecycleListener listener : globals) {
                invokeListenerSafely(listener, event);
            }
        }
    }

    /**
     * Dispatches a lifecycle phase across a collection of active mod IDs in deterministic sequence.
     *
     * @param phase          the target lifecycle phase
     * @param modIds         ordered collection of mod IDs
     * @param contextPayload optional contextual data
     */
    public void dispatchPhase(FmlLifecyclePhase phase, List<String> modIds, Object contextPayload) {
        Objects.requireNonNull(phase, "phase must not be null");
        Objects.requireNonNull(modIds, "modIds must not be null");

        LOGGER.info("[VESSEL-FORGE] Starting lifecycle phase: {} across {} mods", phase, modIds.size());
        for (String modId : modIds) {
            dispatch(phase, modId, contextPayload);
        }
        executedPhases.add(phase);
        LOGGER.info("[VESSEL-FORGE] Completed lifecycle phase: {}", phase);
    }

    /**
     * Returns an unmodifiable list of all phases executed by this dispatcher.
     *
     * @return list of executed phases
     */
    public List<FmlLifecyclePhase> getExecutedPhases() {
        return Collections.unmodifiableList(executedPhases);
    }

    /**
     * Clears all registered listeners and history.
     */
    public void reset() {
        modListeners.clear();
        globalListeners.clear();
        executedPhases.clear();
    }

    private void invokeListenerSafely(FmlLifecycleListener listener, FmlLifecycleEvent event) {
        try {
            listener.onLifecycleEvent(event);
        } catch (Exception ex) {
            LOGGER.error("[VESSEL-FORGE] Failure during {} for mod '{}': {}",
                    event.phase(), event.modId(), ex.getMessage(), ex);
            throw new FmlLifecycleException(
                    ERROR_CODE_LIFECYCLE_TRANSITION,
                    event.modId(),
                    event.phase(),
                    ex.getMessage(),
                    ex
            );
        }
    }
}
