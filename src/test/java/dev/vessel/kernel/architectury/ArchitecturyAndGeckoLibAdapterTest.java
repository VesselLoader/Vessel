package dev.vessel.kernel.architectury;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests validating Architectury synthetic bridge and GeckoLib server runtime adapters.
 *
 * <p>Validates elimination of: ECO-045, ECO-046.</p>
 * <p>Strictly adheres to Rule 17 (zero inner, nested, or anonymous classes).</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ArchitecturyAndGeckoLibAdapterTest {

    private DefaultArchitecturyPlatformProvider provider;
    private ArchitecturySyntheticBridge bridge;
    private GeckoLibRuntimeAdapter geckoAdapter;
    private SampleTrackingPacketListener packetListener;

    @BeforeEach
    public void setUp() {
        provider = new DefaultArchitecturyPlatformProvider();
        bridge = new ArchitecturySyntheticBridge(provider);
        geckoAdapter = new GeckoLibRuntimeAdapter();
        packetListener = new SampleTrackingPacketListener();
        geckoAdapter.registerPacketListener(packetListener);
    }

    @Test
    @DisplayName("ArchitecturySyntheticBridge: Queries platform details and tracks loaded mods")
    public void testPlatformQueries() {
        provider.setPlatformKind(ArchitecturyPlatformKind.FABRIC);
        assertThat(bridge.getPlatformKind()).isEqualTo(ArchitecturyPlatformKind.FABRIC);

        provider.addLoadedMod("architectury");
        provider.addLoadedMod("cloth_config");

        assertThat(bridge.isModLoaded("architectury")).isTrue();
        assertThat(bridge.isModLoaded("CLOTH_CONFIG")).isTrue();
        assertThat(bridge.isModLoaded("unknown_mod")).isFalse();

        assertThat(bridge.getConfigDirectory()).isNotNull();
        assertThat(bridge.getGameDirectory()).isNotNull();
    }

    @Test
    @DisplayName("ArchitecturySyntheticBridge: Resolves @ExpectPlatform synthetic methods")
    public void testExpectPlatformMethodResolution() throws Exception {
        provider.setPlatformKind(ArchitecturyPlatformKind.FABRIC);

        Method resolved = bridge.resolveExpectPlatformMethod(
                SampleExpectPlatformTarget.class,
                "getGreeting"
        );

        assertThat(resolved).isNotNull();
        assertThat(resolved.getDeclaringClass()).isEqualTo(SampleExpectPlatformTargetImpl.class);

        Object result = resolved.invoke(null);
        assertThat(result).isEqualTo(SampleExpectPlatformTargetImpl.EXPECTED_GREETING);
    }

    @Test
    @DisplayName("ArchitecturySyntheticBridge: Throws NoSuchMethodException when platform impl is missing")
    public void testExpectPlatformMissingMethod() {
        assertThatThrownBy(() -> {
            bridge.resolveExpectPlatformMethod(
                    SampleExpectPlatformTarget.class,
                    "nonExistentMethod"
            );
        }).isInstanceOf(NoSuchMethodException.class);
    }

    @Test
    @DisplayName("ArchitecturySyntheticBridge: Registers and retrieves custom synthetic handlers")
    public void testCustomHandlerRouting() {
        String dummyHandler = "NativeNetworkRoutingHandler";
        bridge.registerCustomHandler("network.manager", dummyHandler);

        assertThat(bridge.getCustomHandler("network.manager")).isEqualTo(dummyHandler);
    }

    @Test
    @DisplayName("GeckoLibRuntimeAdapter: Triggers animation and broadcasts network sync packets")
    public void testAnimationTriggerAndSync() {
        GeckoAnimationState state = geckoAdapter.triggerAnimation(
                "entity-dragon-101",
                "attack_controller",
                "bite_attack",
                1000L,
                20L,
                false,
                1.5
        );

        assertThat(state).isNotNull();
        assertThat(state.animatableId()).isEqualTo("entity-dragon-101");
        assertThat(state.controllerName()).isEqualTo("attack_controller");
        assertThat(state.animationName()).isEqualTo("bite_attack");
        assertThat(state.transitionSpeed()).isEqualTo(1.5);
        assertThat(state.loop()).isFalse();

        assertThat(packetListener.getTriggeredAnimations()).hasSize(1);
        assertThat(packetListener.getTriggeredAnimations().get(0))
                .isEqualTo("entity-dragon-101::attack_controller::bite_attack::false");
    }

    @Test
    @DisplayName("GeckoLibRuntimeAdapter: Ticks animation runtime and prunes concluded non-looping animations")
    public void testAnimationTickingAndPruning() {
        geckoAdapter.triggerAnimation(
                "boss-golem",
                "slam_controller",
                "ground_slam",
                500L,
                30L,
                false,
                1.0
        );

        assertThat(geckoAdapter.getActiveAnimationCount()).isEqualTo(1);

        // Tick before conclusion (515 < 500 + 30)
        int prunedEarly = geckoAdapter.tick(515L);
        assertThat(prunedEarly).isEqualTo(0);
        assertThat(geckoAdapter.getActiveAnimationCount()).isEqualTo(1);

        // Tick at or after conclusion (535 >= 500 + 30)
        int prunedConcluded = geckoAdapter.tick(535L);
        assertThat(prunedConcluded).isEqualTo(1);
        assertThat(geckoAdapter.getActiveAnimationCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("GeckoLibRuntimeAdapter: Looping animations are not pruned by regular world ticks")
    public void testLoopingAnimationPersistence() {
        geckoAdapter.triggerAnimation(
                "pet-wolf",
                "idle_controller",
                "tail_wag",
                100L,
                10L,
                true,
                1.0
        );

        // Even far past duration, looping animation stays active
        int pruned = geckoAdapter.tick(5000L);
        assertThat(pruned).isEqualTo(0);
        assertThat(geckoAdapter.getActiveAnimationCount()).isEqualTo(1);

        // Explicit stop
        GeckoAnimationState stopped = geckoAdapter.stopAnimation("pet-wolf", "idle_controller");
        assertThat(stopped).isNotNull();
        assertThat(geckoAdapter.getActiveAnimationCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("GeckoLibRuntimeAdapter: Unregisters packet listeners cleanly")
    public void testUnregisterPacketListener() {
        boolean removed = geckoAdapter.unregisterPacketListener(packetListener);
        assertThat(removed).isTrue();

        geckoAdapter.triggerAnimation("e1", "c1", "a1", 10L, true);
        assertThat(packetListener.getTriggeredAnimations()).isEmpty();
    }
}
