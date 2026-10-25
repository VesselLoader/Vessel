package dev.vessel.kernel.scene;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Handles late-joining player synchronization for dynamic contraptions.
 * <p>
 * Implements Architecture Decision 11:
 * Delivers a one-time full-state packet to players entering range,
 * then seamlessly binds them into the delta tracking broadcast stream.
 *
 * @version 1.0.0
 */
public final class LateJoinFullStateSnapshot {

    private final ContraptionDeltaTracker deltaTracker;

    /**
     * Constructs a late-join handler bound to a delta tracker.
     *
     * @param deltaTracker target contraption delta tracker
     */
    public LateJoinFullStateSnapshot(ContraptionDeltaTracker deltaTracker) {
        this.deltaTracker = Objects.requireNonNull(deltaTracker, "deltaTracker cannot be null");
    }

    /**
     * Synchronizes a newly tracking player by sending a one-time full state packet
     * and adding them to the delta tracker subscriber set.
     *
     * @param playerId     new player identifier
     * @param packetSender consumer delivering (playerId, fullState)
     */
    public void synchronizeLateJoiner(String playerId, BiConsumer<String, DynamicContraptionState> packetSender) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(packetSender, "packetSender cannot be null");

        DynamicContraptionState fullState = deltaTracker.getLastState();

        // 1. Deliver one-time full state packet
        packetSender.accept(playerId, fullState);

        // 2. Add player to tracking set for subsequent delta broadcasts
        deltaTracker.addTracker(playerId);
    }
}
