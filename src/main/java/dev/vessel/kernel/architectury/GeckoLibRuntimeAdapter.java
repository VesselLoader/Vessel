package dev.vessel.kernel.architectury;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Server-side compatibility runtime for GeckoLib animated models and controllers.
 *
 * <p>Maintains controller state machines, ticks non-looping transitions, and orchestrates
 * network synchronization packet broadcasts to tracked clients without client-only render
 * dependencies.</p>
 *
 * <p>Quells: ECO-046.</p>
 * <p>Strictly adheres to Rule 17 (zero inner, nested, or anonymous classes).</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class GeckoLibRuntimeAdapter {

    /**
     * Default playback speed multiplier.
     */
    public static final double DEFAULT_TRANSITION_SPEED = 1.0;

    /**
     * Indefinite duration marker in ticks.
     */
    public static final long INDEFINITE_DURATION = -1L;

    private final Map<String, GeckoAnimationState> activeStates = new ConcurrentHashMap<>();
    private final List<GeckoAnimationPacketListener> packetListeners = new CopyOnWriteArrayList<>();

    /**
     * Constructs a new GeckoLib runtime adapter.
     */
    public GeckoLibRuntimeAdapter() {
    }

    /**
     * Triggers a new animation sequence on the server and broadcasts network sync packets.
     *
     * @param animatableId target entity or block entity ID
     * @param controllerName controller name
     * @param animationName animation clip name
     * @param currentTick world tick at trigger time
     * @param durationTicks expected duration in ticks (-1 if indefinite or looping)
     * @param loop whether to loop indefinitely
     * @param transitionSpeed playback transition speed multiplier
     * @return created animation state record
     */
    public GeckoAnimationState triggerAnimation(
            String animatableId,
            String controllerName,
            String animationName,
            long currentTick,
            long durationTicks,
            boolean loop,
            double transitionSpeed) {
        Objects.requireNonNull(animatableId, "animatableId cannot be null");
        Objects.requireNonNull(controllerName, "controllerName cannot be null");
        Objects.requireNonNull(animationName, "animationName cannot be null");

        String stateKey = computeStateKey(animatableId, controllerName);
        GeckoAnimationState state = new GeckoAnimationState(
                animatableId,
                controllerName,
                animationName,
                currentTick,
                durationTicks,
                loop,
                transitionSpeed
        );

        activeStates.put(stateKey, state);

        for (int i = 0; i < packetListeners.size(); i++) {
            packetListeners.get(i).onAnimationTriggered(
                    animatableId,
                    controllerName,
                    animationName,
                    loop,
                    transitionSpeed
            );
        }

        return state;
    }

    /**
     * Triggers a simple one-shot or looping animation with default speed and duration.
     *
     * @param animatableId target entity or block entity ID
     * @param controllerName controller name
     * @param animationName animation clip name
     * @param currentTick world tick at trigger time
     * @param loop whether to loop
     * @return created animation state record
     */
    public GeckoAnimationState triggerAnimation(
            String animatableId,
            String controllerName,
            String animationName,
            long currentTick,
            boolean loop) {
        return triggerAnimation(
                animatableId,
                controllerName,
                animationName,
                currentTick,
                INDEFINITE_DURATION,
                loop,
                DEFAULT_TRANSITION_SPEED
        );
    }

    /**
     * Stops an active animation controller for an entity.
     *
     * @param animatableId target entity or block entity ID
     * @param controllerName controller name
     * @return previous state if present, or null
     */
    public GeckoAnimationState stopAnimation(String animatableId, String controllerName) {
        Objects.requireNonNull(animatableId, "animatableId cannot be null");
        Objects.requireNonNull(controllerName, "controllerName cannot be null");
        return activeStates.remove(computeStateKey(animatableId, controllerName));
    }

    /**
     * Ticks the animation runtime, pruning concluded non-looping animations.
     *
     * @param currentTick current world tick
     * @return number of concluded animations pruned during this tick
     */
    public int tick(long currentTick) {
        int prunedCount = 0;
        Iterator<Map.Entry<String, GeckoAnimationState>> iterator = activeStates.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, GeckoAnimationState> entry = iterator.next();
            GeckoAnimationState state = entry.getValue();
            if (state.hasConcluded(currentTick)) {
                iterator.remove();
                prunedCount++;
            }
        }

        return prunedCount;
    }

    /**
     * Retrieves the active animation state for a given controller.
     *
     * @param animatableId target entity ID
     * @param controllerName controller name
     * @return optional animation state
     */
    public Optional<GeckoAnimationState> getAnimationState(String animatableId, String controllerName) {
        Objects.requireNonNull(animatableId, "animatableId cannot be null");
        Objects.requireNonNull(controllerName, "controllerName cannot be null");
        return Optional.ofNullable(activeStates.get(computeStateKey(animatableId, controllerName)));
    }

    /**
     * Registers a network synchronization packet listener.
     *
     * @param listener packet listener
     */
    public void registerPacketListener(GeckoAnimationPacketListener listener) {
        Objects.requireNonNull(listener, "listener cannot be null");
        packetListeners.add(listener);
    }

    /**
     * Unregisters a network synchronization packet listener.
     *
     * @param listener packet listener
     * @return true if removed, false otherwise
     */
    public boolean unregisterPacketListener(GeckoAnimationPacketListener listener) {
        Objects.requireNonNull(listener, "listener cannot be null");
        return packetListeners.remove(listener);
    }

    /**
     * Returns an unmodifiable map of active animation states.
     *
     * @return unmodifiable map of active states
     */
    public Map<String, GeckoAnimationState> getActiveStates() {
        return Collections.unmodifiableMap(activeStates);
    }

    /**
     * Returns the count of active animations.
     *
     * @return active animation count
     */
    public int getActiveAnimationCount() {
        return activeStates.size();
    }

    private static String computeStateKey(String animatableId, String controllerName) {
        return animatableId + "#" + controllerName;
    }
}
