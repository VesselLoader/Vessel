package dev.vessel.kernel.test;

import dev.vessel.kernel.scene.ContraptionDeltaTracker;
import dev.vessel.kernel.scene.DynamicContraptionState;
import dev.vessel.kernel.scene.LateJoinFullStateSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit verification suite for Architecture Decision 11: Dynamic Contraption Sync & Late Join.
 *
 * @version 1.0.0
 */
public class DynamicContraptionSyncTest {

    @Test
    @DisplayName("DynamicContraptionState tracks dirty flags for delta compression")
    void testStateFlags() {
        DynamicContraptionState state = new DynamicContraptionState(
                1001L, 10.0, 64.0, 20.0, 90.0f, 0.0f, 0.0f, 1L,
                DynamicContraptionState.DIRTY_POS | DynamicContraptionState.DIRTY_ROT
        );

        assertThat(state.contraptionId()).isEqualTo(1001L);
        assertThat(state.hasPositionChanged()).isTrue();
        assertThat(state.hasRotationChanged()).isTrue();
        assertThat(state.hasStructureChanged()).isFalse();
        assertThat(state.isClean()).isFalse();
    }

    @Test
    @DisplayName("Decision 11: ContraptionDeltaTracker broadcasts single delta to all tracking players with zero recomputation")
    void testDeltaBroadcast() {
        DynamicContraptionState initial = new DynamicContraptionState(1001L, 0, 0, 0, 0, 0, 0, 1L, 0);
        ContraptionDeltaTracker tracker = new ContraptionDeltaTracker(initial);

        tracker.addTracker("PlayerA");
        tracker.addTracker("PlayerB");
        tracker.addTracker("PlayerC");
        assertThat(tracker.getTrackerCount()).isEqualTo(3);

        List<String> receivedPlayers = new ArrayList<>();
        DynamicContraptionState moved = new DynamicContraptionState(
                1001L, 5.0, 0, 0, 0, 0, 0, 1L, DynamicContraptionState.DIRTY_POS
        );

        int sent = tracker.broadcastDelta(moved, (playerId, state) -> {
            receivedPlayers.add(playerId);
            assertThat(state.posX()).isEqualTo(5.0);
        });

        assertThat(sent).isEqualTo(3);
        assertThat(receivedPlayers).containsExactlyInAnyOrder("PlayerA", "PlayerB", "PlayerC");
        assertThat(tracker.getTotalBroadcasts()).isEqualTo(1L);
        assertThat(tracker.getTotalPacketsDispatched()).isEqualTo(3L);
        assertThat(tracker.getLastState()).isEqualTo(moved);
    }

    @Test
    @DisplayName("Decision 11: LateJoinFullStateSnapshot sends one-time full state packet and binds tracker")
    void testLateJoiner() {
        DynamicContraptionState current = new DynamicContraptionState(1001L, 100.0, 70.0, 200.0, 45.0f, 0.0f, 0.0f, 5L, 0);
        ContraptionDeltaTracker tracker = new ContraptionDeltaTracker(current);
        LateJoinFullStateSnapshot lateJoin = new LateJoinFullStateSnapshot(tracker);

        List<String> fullStateReceived = new ArrayList<>();

        lateJoin.synchronizeLateJoiner("LateJoinerPlayer", (playerId, state) -> {
            fullStateReceived.add(playerId);
            assertThat(state.posX()).isEqualTo(100.0);
            assertThat(state.structureVersion()).isEqualTo(5L);
        });

        assertThat(fullStateReceived).containsExactly("LateJoinerPlayer");
        // Verify player is now in the tracking set for subsequent deltas
        assertThat(tracker.getTrackingPlayers()).contains("LateJoinerPlayer");
    }
}
