package dev.vessel.kernel.adapter.forge;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests verifying FML lifecycle event dispatching, custom payload pipeline routing,
 * and inter-mod communications (IMC) mechanisms.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ForgeLifecycleAndPipelineTest {

    private FmlLifecycleEventDispatcher lifecycleDispatcher;
    private ForgeChannelPipelineBridge pipelineBridge;
    private ForgeInterModCommsRouter imcRouter;

    @BeforeEach
    public void setUp() {
        lifecycleDispatcher = new FmlLifecycleEventDispatcher();
        pipelineBridge = new ForgeChannelPipelineBridge(1024); // 1KB test buffer
        imcRouter = new ForgeInterModCommsRouter();
    }

    @Test
    @DisplayName("FML Lifecycle - Sequences phases deterministically across mods")
    public void testLifecycleSequencing() {
        List<String> eventLog = new ArrayList<>();

        lifecycleDispatcher.registerListener("mod_alpha", FmlLifecyclePhase.CONSTRUCT_MOD,
                ev -> eventLog.add("mod_alpha:CONSTRUCT"));
        lifecycleDispatcher.registerListener("mod_alpha", FmlLifecyclePhase.COMMON_SETUP,
                ev -> eventLog.add("mod_alpha:SETUP"));
        lifecycleDispatcher.registerGlobalListener(FmlLifecyclePhase.INTER_MOD_ENQUEUE,
                ev -> eventLog.add(ev.modId() + ":ENQUEUE"));

        List<String> mods = List.of("mod_alpha", "mod_beta");

        lifecycleDispatcher.dispatchPhase(FmlLifecyclePhase.CONSTRUCT_MOD, mods, null);
        lifecycleDispatcher.dispatchPhase(FmlLifecyclePhase.COMMON_SETUP, mods, null);
        lifecycleDispatcher.dispatchPhase(FmlLifecyclePhase.INTER_MOD_ENQUEUE, mods, null);

        assertThat(eventLog).containsExactly(
                "mod_alpha:CONSTRUCT",
                "mod_alpha:SETUP",
                "mod_alpha:ENQUEUE",
                "mod_beta:ENQUEUE"
        );
        assertThat(lifecycleDispatcher.getExecutedPhases()).containsExactly(
                FmlLifecyclePhase.CONSTRUCT_MOD,
                FmlLifecyclePhase.COMMON_SETUP,
                FmlLifecyclePhase.INTER_MOD_ENQUEUE
        );
    }

    @Test
    @DisplayName("FML Lifecycle - Throws typed FmlLifecycleException with ERR-61 on handler failure")
    public void testLifecycleExceptionSuppression() {
        lifecycleDispatcher.registerListener("faulty_mod", FmlLifecyclePhase.COMMON_SETUP, ev -> {
            throw new IllegalStateException("Simulated crash in setup");
        });

        assertThatThrownBy(() -> lifecycleDispatcher.dispatch(FmlLifecyclePhase.COMMON_SETUP, "faulty_mod", null))
                .isInstanceOf(FmlLifecycleException.class)
                .hasMessageContaining("[ERR-61]")
                .hasMessageContaining("faulty_mod");
    }

    @Test
    @DisplayName("Forge Channel Pipeline - Successfully registers and routes custom packets")
    public void testChannelPipelineRouting() {
        AtomicBoolean packetReceived = new AtomicBoolean(false);
        String channel = "create:contraption_packet";

        pipelineBridge.registerChannel(channel, msg -> {
            packetReceived.set(true);
            assertThat(msg.channelName()).isEqualTo(channel);
            assertThat(new String(msg.payload(), StandardCharsets.UTF_8)).isEqualTo("contraption_data");
            assertThat(msg.direction()).isEqualTo(ForgeNetworkDirection.PLAY_TO_SERVER);
        });

        assertThat(pipelineBridge.isChannelRegistered(channel)).isTrue();

        ForgeChannelMessage message = new ForgeChannelMessage(
                channel,
                "contraption_data".getBytes(StandardCharsets.UTF_8),
                ForgeNetworkDirection.PLAY_TO_SERVER,
                "player-session-123"
        );

        pipelineBridge.routeMessage(message);
        assertThat(packetReceived.get()).isTrue();
    }

    @Test
    @DisplayName("Forge Channel Pipeline - Rejects unregistered channels with ECO-011")
    public void testUnregisteredChannelRejection() {
        ForgeChannelMessage message = new ForgeChannelMessage(
                "unregistered:channel",
                new byte[]{1, 2, 3},
                ForgeNetworkDirection.PLAY_TO_SERVER,
                "session-1"
        );

        assertThatThrownBy(() -> pipelineBridge.routeMessage(message))
                .isInstanceOf(ForgeChannelPipelineException.class)
                .hasMessageContaining("[ECO-011]");
    }

    @Test
    @DisplayName("Forge Channel Pipeline - Enforces max payload bounds with ERR-91")
    public void testPayloadSizeLimitViolation() {
        String channel = "test:large_payload";
        pipelineBridge.registerChannel(channel, msg -> {});

        byte[] oversized = new byte[2048]; // exceeds 1024 limit
        ForgeChannelMessage message = new ForgeChannelMessage(
                channel,
                oversized,
                ForgeNetworkDirection.PLAY_TO_SERVER,
                "session-1"
        );

        assertThatThrownBy(() -> pipelineBridge.routeMessage(message))
                .isInstanceOf(ForgeChannelPipelineException.class)
                .hasMessageContaining("[ERR-91]")
                .hasMessageContaining("exceeds limit");
    }

    @Test
    @DisplayName("Forge IMC Router - Queues and queries inter-mod messages")
    public void testInterModCommsRouting() {
        imcRouter.transitionStage(InterModCommsLifecycleStage.ENQUEUE_OPEN);

        imcRouter.sendTo("curios", "ironchest", "register_slot", "ring");
        imcRouter.sendTo("curios", "ironchest", "register_slot", "necklace");
        imcRouter.sendTo("jei", "ironchest", "register_recipe", "chest_recipe");

        assertThat(imcRouter.getMessages("ironchest")).hasSize(3);
        assertThat(imcRouter.getMessages("ironchest", "register_slot")).hasSize(2);
        assertThat(imcRouter.getMessages("ironchest", "register_recipe")).hasSize(1);
    }

    @Test
    @DisplayName("Forge IMC Router - Enforces lifecycle stage gates with ECO-005")
    public void testImcLifecycleGatekeeper() {
        imcRouter.transitionStage(InterModCommsLifecycleStage.CLOSED);

        assertThatThrownBy(() -> imcRouter.sendTo("mod_a", "mod_b", "action", "data"))
                .isInstanceOf(ForgeInterModCommsException.class)
                .hasMessageContaining("[ECO-005]")
                .hasMessageContaining("CLOSED");
    }
}
