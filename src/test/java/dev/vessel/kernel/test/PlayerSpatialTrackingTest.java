package dev.vessel.kernel.test;

import dev.vessel.kernel.actor.RegionCoordinateHash;
import dev.vessel.kernel.player.EntityRegionTransitionTracker;
import dev.vessel.kernel.player.PlayerSpatialPosition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit verification suite for player spatial positions and region boundary transitions.
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-6)
 */
public class PlayerSpatialTrackingTest {

    @Test
    @DisplayName("PlayerSpatialPosition pre-computes region hash accurately from coordinates")
    void testPlayerSpatialPosition() {
        UUID playerId = UUID.randomUUID();
        // Block coordinate 1024, 2048 -> regionX = 1024 >> 8 = 4, regionZ = 2048 >> 8 = 8
        PlayerSpatialPosition pos = PlayerSpatialPosition.of(playerId, 1024.5, 64.0, 2048.5, 90.0f, 0.0f);

        assertThat(pos.playerId()).isEqualTo(playerId);
        assertThat(pos.x()).isEqualTo(1024.5);
        assertThat(pos.y()).isEqualTo(64.0);
        assertThat(pos.z()).isEqualTo(2048.5);
        assertThat(pos.regionHash().regionX()).isEqualTo(4);
        assertThat(pos.regionHash().regionZ()).isEqualTo(8);
    }

    @Test
    @DisplayName("EntityRegionTransitionTracker detects boundary transitions across 16x16 chunk regions")
    void testBoundaryTransitions() {
        EntityRegionTransitionTracker tracker = new EntityRegionTransitionTracker();
        UUID playerA = UUID.randomUUID();

        // 1. Initial position in region (0, 0)
        Optional<RegionCoordinateHash> trans1 = tracker.updatePosition(playerA, 10.0, 64.0, 10.0, 0.0f, 0.0f);
        assertThat(trans1).isEmpty();
        assertThat(tracker.getRegion(playerA)).isEqualTo(RegionCoordinateHash.fromBlock(10, 10));

        // 2. Small movement within same region -> no transition
        Optional<RegionCoordinateHash> trans2 = tracker.updatePosition(playerA, 100.0, 64.0, 100.0, 10.0f, 0.0f);
        assertThat(trans2).isEmpty();

        // 3. Movement across boundary to block 500 (regionX = 500 >> 8 = 1) -> transition triggered!
        Optional<RegionCoordinateHash> trans3 = tracker.updatePosition(playerA, 500.0, 64.0, 10.0, 10.0f, 0.0f);
        assertThat(trans3).isPresent();
        assertThat(trans3.get()).isEqualTo(RegionCoordinateHash.fromBlock(10, 10));
        assertThat(tracker.getRegion(playerA)).isEqualTo(RegionCoordinateHash.fromBlock(500, 10));

        // 4. Remove entity on disconnect
        tracker.removeEntity(playerA);
        assertThat(tracker.activeEntityCount()).isEqualTo(0);
        assertThat(tracker.getPosition(playerA)).isNull();
    }
}
