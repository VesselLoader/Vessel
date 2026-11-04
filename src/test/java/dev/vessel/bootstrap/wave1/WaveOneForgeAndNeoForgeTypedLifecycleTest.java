package dev.vessel.bootstrap.wave1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vessel.kernel.event.orchestration.ForgeAndNeoForgeLifecycleSequenceDispatcher;
import java.util.List;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Wave 1 ({@code ADR-0002} &amp; {@code ADR-0003}) verifying typed
 * MinecraftForge and NeoForge lifecycle event sequences
 * ({@code FMLConstructModEvent}, {@code FMLCommonSetupEvent},
 * {@code FMLDedicatedServerSetupEvent}, {@code FMLLoadCompleteEvent}), deferred work queue
 * draining via {@code enqueueWork(Runnable)}, and {@code @SubscribeEvent} priority ordering
 * without {@link ClassCastException}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
final class WaveOneForgeAndNeoForgeTypedLifecycleTest {

    private FMLCommonSetupEvent capturedForgeFunctionalEvent;
    private net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent capturedNeoForgeFunctionalEvent;

    public void recordFunctionalForgeCommonSetup(FMLCommonSetupEvent event) {
        this.capturedForgeFunctionalEvent = event;
    }

    public void recordFunctionalNeoForgeCommonSetup(
            net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        this.capturedNeoForgeFunctionalEvent = event;
    }

    @Test
    void testForgeTypedLifecycleSequencePriorityOrderAndDeferredWorkQueue() {
        net.minecraftforge.eventbus.api.IEventBus forgeBus =
                new net.minecraftforge.eventbus.api.VesselEventBus();
        SampleForgeWaveOneModFixture forgeFixture = new SampleForgeWaveOneModFixture();
        forgeBus.register(forgeFixture);
        forgeBus.addListener(
                net.minecraftforge.eventbus.api.EventPriority.HIGHEST,
                false,
                FMLCommonSetupEvent.class,
                this::recordFunctionalForgeCommonSetup);

        ForgeAndNeoForgeLifecycleSequenceDispatcher dispatcher =
                new ForgeAndNeoForgeLifecycleSequenceDispatcher();
        int dispatchedPhases =
                dispatcher.dispatchForgeModLifecycleSequence("wave1_forge_test", forgeBus);

        assertEquals(4, dispatchedPhases);
        assertNotNull(capturedForgeFunctionalEvent);
        assertEquals("wave1_forge_test", capturedForgeFunctionalEvent.getModId());
        assertEquals(
                "FMLCommonSetupEvent[wave1_forge_test]",
                capturedForgeFunctionalEvent.description());

        List<String> log = forgeFixture.getLifecycleLog();
        assertEquals(9, log.size(), "Expected 8 @SubscribeEvent entries + 1 enqueued work task");

        int constructHighIdx = log.indexOf("FORGE_CONSTRUCT_HIGH:wave1_forge_test");
        int constructLowIdx = log.indexOf("FORGE_CONSTRUCT_LOW:wave1_forge_test");
        int commonHighIdx = log.indexOf("FORGE_COMMON_SETUP_HIGH:wave1_forge_test");
        int commonLowIdx = log.indexOf("FORGE_COMMON_SETUP_LOW:wave1_forge_test");
        int enqueuedWorkIdx = log.indexOf("FORGE_ENQUEUED_WORK");
        int dedicatedHighIdx = log.indexOf("FORGE_DEDICATED_SERVER_HIGH:wave1_forge_test");
        int dedicatedLowIdx = log.indexOf("FORGE_DEDICATED_SERVER_LOW:wave1_forge_test");
        int loadCompleteHighIdx = log.indexOf("FORGE_LOAD_COMPLETE_HIGH:wave1_forge_test");
        int loadCompleteLowIdx = log.indexOf("FORGE_LOAD_COMPLETE_LOW:wave1_forge_test");

        assertTrue(constructHighIdx >= 0 && constructHighIdx < constructLowIdx);
        assertTrue(constructLowIdx < commonHighIdx);
        assertTrue(commonHighIdx < commonLowIdx);
        assertTrue(enqueuedWorkIdx > commonHighIdx && enqueuedWorkIdx < dedicatedHighIdx);
        assertTrue(dedicatedHighIdx < dedicatedLowIdx);
        assertTrue(dedicatedLowIdx < loadCompleteHighIdx);
        assertTrue(loadCompleteHighIdx < loadCompleteLowIdx);
    }

    @Test
    void testNeoForgeTypedLifecycleSequencePriorityOrderAndDeferredWorkQueue() {
        net.neoforged.bus.api.IEventBus neoForgeBus = new net.neoforged.bus.api.VesselNeoEventBus();
        SampleNeoForgeWaveOneModFixture neoForgeFixture =
                new SampleNeoForgeWaveOneModFixture(neoForgeBus);
        neoForgeBus.addListener(
                net.neoforged.bus.api.EventPriority.HIGHEST,
                false,
                net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent.class,
                this::recordFunctionalNeoForgeCommonSetup);

        ForgeAndNeoForgeLifecycleSequenceDispatcher dispatcher =
                new ForgeAndNeoForgeLifecycleSequenceDispatcher();
        int dispatchedPhases =
                dispatcher.dispatchNeoForgeModLifecycleSequence("wave1_neoforge_test", neoForgeBus);

        assertEquals(4, dispatchedPhases);
        assertNotNull(capturedNeoForgeFunctionalEvent);
        assertEquals("wave1_neoforge_test", capturedNeoForgeFunctionalEvent.getModId());
        assertEquals(
                "FMLCommonSetupEvent[wave1_neoforge_test]",
                capturedNeoForgeFunctionalEvent.description());

        List<String> log = neoForgeFixture.getLifecycleLog();
        assertEquals(9, log.size(), "Expected 8 @SubscribeEvent entries + 1 enqueued work task");

        int constructHighIdx = log.indexOf("NEOFORGE_CONSTRUCT_HIGH:wave1_neoforge_test");
        int constructLowIdx = log.indexOf("NEOFORGE_CONSTRUCT_LOW:wave1_neoforge_test");
        int commonHighIdx = log.indexOf("NEOFORGE_COMMON_SETUP_HIGH:wave1_neoforge_test");
        int commonLowIdx = log.indexOf("NEOFORGE_COMMON_SETUP_LOW:wave1_neoforge_test");
        int enqueuedWorkIdx = log.indexOf("NEOFORGE_ENQUEUED_WORK");
        int dedicatedHighIdx = log.indexOf("NEOFORGE_DEDICATED_SERVER_HIGH:wave1_neoforge_test");
        int dedicatedLowIdx = log.indexOf("NEOFORGE_DEDICATED_SERVER_LOW:wave1_neoforge_test");
        int loadCompleteHighIdx = log.indexOf("NEOFORGE_LOAD_COMPLETE_HIGH:wave1_neoforge_test");
        int loadCompleteLowIdx = log.indexOf("NEOFORGE_LOAD_COMPLETE_LOW:wave1_neoforge_test");

        assertTrue(constructHighIdx >= 0 && constructHighIdx < constructLowIdx);
        assertTrue(constructLowIdx < commonHighIdx);
        assertTrue(commonHighIdx < commonLowIdx);
        assertTrue(enqueuedWorkIdx > commonHighIdx && enqueuedWorkIdx < dedicatedHighIdx);
        assertTrue(dedicatedHighIdx < dedicatedLowIdx);
        assertTrue(dedicatedLowIdx < loadCompleteHighIdx);
        assertTrue(loadCompleteHighIdx < loadCompleteLowIdx);
    }
}
