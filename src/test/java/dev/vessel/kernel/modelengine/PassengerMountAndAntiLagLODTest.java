package dev.vessel.kernel.modelengine;

import dev.vessel.kernel.modelengine.lod.DisplayInterpolationConfig;
import dev.vessel.kernel.modelengine.lod.ModelAnimationLODGovernor;
import dev.vessel.kernel.modelengine.lod.ModelAnimationLODTier;
import dev.vessel.kernel.modelengine.lod.MovementDeltaCullingFilter;
import dev.vessel.kernel.modelengine.lod.PassengerMountTracker;
import dev.vessel.kernel.modelengine.lod.PassengerPacketGenerator;
import dev.vessel.kernel.optimization.entity.PlayerSpatialCoordinates;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verification test suite for Anti-Lag Passenger Mounting, Client GPU Interpolation,
 * Activation Range LOD, and Delta Threshold Culling.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class PassengerMountAndAntiLagLODTest {

    @Test
    @DisplayName("PassengerMountTracker achieves over 90% network packet reduction with passenger mounting")
    void testPassengerMountTrackerPacketSavings() {
        int driverId = 100;
        PassengerMountTracker tracker = new PassengerMountTracker(driverId);

        // Mount 15 bone entities
        for (int i = 1; i <= 15; i++) {
            tracker.addPassenger(100 + i);
        }

        assertThat(tracker.getPassengerCount()).isEqualTo(15);

        // For 15 passengers: 15 / (15 + 1) = 15/16 = 93.75% packet reduction!
        double ratio = tracker.calculatePacketReductionRatio();
        assertThat(ratio).isGreaterThanOrEqualTo(0.90);
        assertThat(ratio).isEqualTo(15.0 / 16.0);

        // Record 20 movement ticks
        for (int t = 0; t < 20; t++) {
            tracker.recordMovementTick();
        }

        assertThat(tracker.getTotalMovementTicks()).isEqualTo(20);
        assertThat(tracker.getSavedTeleportPackets()).isEqualTo(300); // 20 * 15 saved packets!
    }

    @Test
    @DisplayName("PassengerPacketGenerator correctly encodes and decodes passenger mount payloads")
    void testPassengerPacketGeneratorEncoding() {
        PassengerPacketGenerator generator = new PassengerPacketGenerator();
        List<Integer> passengers = List.of(201, 202, 203, 204);

        byte[] payload = generator.encodePassengerPacket(101, passengers);
        assertThat(payload).isNotNull();
        assertThat(payload.length).isEqualTo(4 + 4 + (4 * 4)); // 24 bytes

        int[] decoded = generator.decodePassengerIds(payload);
        assertThat(decoded).containsExactly(201, 202, 203, 204);
    }

    @Test
    @DisplayName("DisplayInterpolationConfig configures client GPU smooth 60 FPS motion without server flooding")
    void testDisplayInterpolationConfig() {
        DisplayInterpolationConfig config = new DisplayInterpolationConfig(3);

        assertThat(config.getInterpolationDurationTicks()).isEqualTo(3);
        assertThat(config.getInterpolationDurationMillis()).isEqualTo(150L);
        assertThat(config.getStartInterpolationTicks()).isEqualTo(0);
        assertThat(config.getTeleportDurationTicks()).isEqualTo(0);

        config.setInterpolationDurationTicks(2);
        assertThat(config.getInterpolationDurationTicks()).isEqualTo(2);
        assertThat(config.getInterpolationDurationMillis()).isEqualTo(100L);

        // Negative ticks throw
        assertThatThrownBy(() -> config.setInterpolationDurationTicks(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("ModelAnimationLODGovernor assigns NEAR (20 TPS), MID (5 TPS), and FAR/DORMANT tiers")
    void testModelAnimationLODGovernor() {
        ModelAnimationLODGovernor governor = new ModelAnimationLODGovernor();

        // Distance < 16m -> NEAR (20 TPS)
        ModelAnimationLODTier nearTier = governor.resolveTier(10.0 * 10.0);
        assertThat(nearTier).isEqualTo(ModelAnimationLODTier.NEAR);
        assertThat(nearTier.getUpdatesPerSecond()).isEqualTo(20);
        assertThat(nearTier.shouldUpdateAnimation(100, 1)).isTrue();

        // Distance 20m (between 16m and 32m) -> MID (5 TPS, every 4 ticks)
        ModelAnimationLODTier midTier = governor.resolveTier(20.0 * 20.0);
        assertThat(midTier).isEqualTo(ModelAnimationLODTier.MID);
        assertThat(midTier.getUpdatesPerSecond()).isEqualTo(5);
        // Staggered check: (tick + entityId) % 4 == 0
        assertThat(midTier.shouldUpdateAnimation(4, 0)).isTrue();
        assertThat(midTier.shouldUpdateAnimation(5, 0)).isFalse();

        // Distance 40m (> 32m) -> FAR (0 TPS, frozen bones)
        ModelAnimationLODTier farTier = governor.resolveTier(40.0 * 40.0);
        assertThat(farTier).isEqualTo(ModelAnimationLODTier.FAR);
        assertThat(farTier.getUpdatesPerSecond()).isEqualTo(0);
        assertThat(farTier.shouldUpdateAnimation(100, 1)).isFalse();

        // Multi-player spatial query
        List<PlayerSpatialCoordinates> players = List.of(
                new PlayerSpatialCoordinates(10.0, 64.0, 10.0), // 10m away from (0, 64, 10)
                new PlayerSpatialCoordinates(100.0, 64.0, 100.0)
        );
        ModelAnimationLODTier resolved = governor.resolveTier(0.0, 64.0, 10.0, players);
        assertThat(resolved).isEqualTo(ModelAnimationLODTier.NEAR);

        // No players -> DORMANT (0 CPU overhead)
        ModelAnimationLODTier dormant = governor.resolveTier(0.0, 64.0, 10.0, Collections.emptyList());
        assertThat(dormant).isEqualTo(ModelAnimationLODTier.DORMANT);
    }

    @Test
    @DisplayName("MovementDeltaCullingFilter suppresses updates below 0.05 blocks and 0.02 radians")
    void testMovementDeltaCullingFilter() {
        MovementDeltaCullingFilter filter = new MovementDeltaCullingFilter();

        // First invocation always dispatches
        assertThat(filter.shouldDispatchUpdate(0.0, 0.0, 0.0, 0.0f, 0.0f)).isTrue();

        // Micro-movement below 0.05 blocks (0.02 blocks move) and rotation below 0.02 radians -> CULLED!
        boolean culled1 = filter.shouldDispatchUpdate(0.02, 0.0, 0.0, 0.5f, 0.0f);
        assertThat(culled1).isFalse();
        assertThat(filter.getCulledUpdatesCount()).isEqualTo(1);

        // Significant movement >= 0.05 blocks (0.1 blocks move) -> DISPATCHED!
        boolean dispatchedMove = filter.shouldDispatchUpdate(0.1, 0.0, 0.0, 0.0f, 0.0f);
        assertThat(dispatchedMove).isTrue();
        assertThat(filter.getAcceptedUpdatesCount()).isEqualTo(2);

        // Micro-movement but significant rotation >= 0.02 rad (~2.0 degrees) -> DISPATCHED!
        boolean dispatchedRot = filter.shouldDispatchUpdate(0.1, 0.0, 0.0, 3.0f, 0.0f);
        assertThat(dispatchedRot).isTrue();
        assertThat(filter.getAcceptedUpdatesCount()).isEqualTo(3);
    }
}
