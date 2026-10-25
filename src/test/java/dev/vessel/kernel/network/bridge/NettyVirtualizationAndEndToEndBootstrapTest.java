package dev.vessel.kernel.network.bridge;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vessel.bootstrap.target.VesselCliFlagsParser;
import dev.vessel.bootstrap.target.VesselServerLauncher;
import dev.vessel.kernel.diagnostic.VesselCrashReportGenerator;
import dev.vessel.kernel.diagnostic.VesselWatchdogThread;
import dev.vessel.kernel.identity.SourceLoaderKind;
import dev.vessel.kernel.identity.VesselModuleIdentifier;
import dev.vessel.kernel.registry.CompactCustomBlockStateIndex;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for Sub-Stage 6.14: Netty packet virtualization, Bedrock RakNet port negotiation,
 * animation coalescing, microblock RLE compression, Smart Blame crash diagnostics, watchdog
 * thread monitoring, and end-to-end server bootstrap verification.
 *
 * @version 1.0.3
 */
class NettyVirtualizationAndEndToEndBootstrapTest {

    @Test
    void testNettyChannelInterceptorBlockVirtualizerPackServerAndBedrockPortBinder() {
        VesselNettyChannelInterceptor interceptor = new VesselNettyChannelInterceptor();
        UUID playerUuid = UUID.randomUUID();
        byte[] payload = "hello".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(
                payload,
                interceptor.interceptOutboundPacket(playerUuid, "create:contraption", payload, false));
        assertEquals(
                0,
                interceptor.interceptOutboundPacket(playerUuid, "create:contraption", payload, true).length);
        assertTrue(interceptor.interceptOutboundPacket(
                playerUuid, "vessel:sync", payload, true).length > payload.length);

        assertTrue(interceptor.interceptInboundPacket(playerUuid, "minecraft:custom", payload, 2));
        assertTrue(interceptor.interceptInboundPacket(playerUuid, "minecraft:custom", payload, 2));
        assertFalse(interceptor.interceptInboundPacket(playerUuid, "minecraft:custom", payload, 2));

        CompactCustomBlockStateIndex stateIndex = new CompactCustomBlockStateIndex();
        stateIndex.registerCustomBlockState(500, "mekanism:osmium_ore", 42);
        ClientboundStateVirtualizer virtualizer = new ClientboundStateVirtualizer();
        assertEquals(42, virtualizer.virtualizeCustomBlockStateForVanilla(500, stateIndex));
        assertEquals("VANILLA_JSON", virtualizer.classifyCustomModelComplexity(false, true));
        assertEquals("DISPLAY_BACKEND", virtualizer.classifyCustomModelComplexity(true, false));
        assertEquals("GREY_CASE_WITH_WARNING", virtualizer.classifyCustomModelComplexity(true, true));

        IntegratedPackHttpServer packServer = new IntegratedPackHttpServer();
        String sha1 = "a1b2c3d4e5f60123456789abcdef0123456789ab";
        packServer.registerCompiledPack(sha1, new byte[]{10, 20, 30, 40});
        assertArrayEquals(new byte[]{10, 20, 30, 40}, packServer.serveCompiledPackSlice(sha1, 0));
        assertEquals(
                "http://127.0.0.1:8085/vessel-pack/" + sha1 + ".zip",
                packServer.buildPackDownloadUrl("127.0.0.1", 8085, sha1));

        BedrockRakNetPortBinder rakNetBinder = new BedrockRakNetPortBinder();
        assertTrue(rakNetBinder.bindOrYieldToExternalGeyser(19132, false));
        assertTrue(rakNetBinder.isInternalRakNetBound());
        assertFalse(rakNetBinder.bindOrYieldToExternalGeyser(19132, true));
        assertFalse(rakNetBinder.isInternalRakNetBound());
    }

    @Test
    void testChatHarmonizerPersonaNegotiatorAnimationCoalescerMicroblockRleAndSortAcceptor() {
        AdventureToBedrockChatHarmonizer chatHarmonizer = new AdventureToBedrockChatHarmonizer();
        assertEquals(
                "§a§lVESSEL §cBedrock",
                chatHarmonizer.convertMiniMessageToBedrockLegacy("<green><bold>VESSEL <red>Bedrock"));

        ClientPersonaNegotiator personaNegotiator = new ClientPersonaNegotiator();
        assertEquals(
                "BEDROCK_RAKNET",
                personaNegotiator.negotiateClientPersona(Set.of(), true));
        assertEquals(
                "MODDED_NATIVE",
                personaNegotiator.negotiateClientPersona(Set.of("create:network"), false));
        assertEquals(
                "VANILLA_DISPLAY_ENTITY",
                personaNegotiator.negotiateClientPersona(Set.of("minecraft:brand"), false));

        AnimationPacketCoalescer animCoalescer = new AnimationPacketCoalescer();
        animCoalescer.queueBoneTransformUpdate(101, "head", 10.0f, 20.0f, 0.0f);
        animCoalescer.queueBoneTransformUpdate(101, "head", 15.0f, 25.0f, 5.0f);
        Map<String, float[]> frame = animCoalescer.flushCoalescedFrame(101);
        assertArrayEquals(new float[]{15.0f, 25.0f, 5.0f}, frame.get("head"), 1e-4f);
        assertTrue(animCoalescer.flushCoalescedFrame(101).isEmpty());

        MicroblockDeltaCompressor rleCompressor = new MicroblockDeltaCompressor();
        long[] rawVoxelWords = new long[64];
        for (int i = 0; i < 64; i++) {
            rawVoxelWords[i] = (i < 60) ? 0xFFFFFFFFFFFFFFFFL : 0x00000000000000FFL;
        }
        int[] compressed = rleCompressor.compressVoxelBitmaskRle(rawVoxelWords);
        assertEquals(6, compressed.length);
        long[] decompressed = rleCompressor.decompressVoxelBitmaskRle(compressed, 64);
        assertArrayEquals(rawVoxelWords, decompressed);

        ContainerTransactionBatchAcceptor sortAcceptor = new ContainerTransactionBatchAcceptor();
        UUID playerUuid = UUID.randomUUID();
        assertTrue(sortAcceptor.acceptRapidSortClick(playerUuid, 1000L, 2));
        assertTrue(sortAcceptor.acceptRapidSortClick(playerUuid, 1100L, 2));
        assertFalse(sortAcceptor.acceptRapidSortClick(playerUuid, 1200L, 2));
        assertTrue(sortAcceptor.acceptRapidSortClick(playerUuid, 1600L, 2));
    }

    @Test
    void testSmartBlameCrashReportWatchdogAndEndToEndBootstrap(@TempDir Path tempDir) {
        VesselCrashReportGenerator crashGenerator = new VesselCrashReportGenerator();
        VesselModuleIdentifier createId =
                VesselModuleIdentifier.of(SourceLoaderKind.FORGE, "create", "0.5.1.f");
        RuntimeException simulatedCrash = new RuntimeException("Kinetic network stress overflow");
        simulatedCrash.setStackTrace(new StackTraceElement[]{
            new StackTraceElement(
                    "com.simibubi.create.content.kinetics.KineticNetwork",
                    "tick",
                    "KineticNetwork.java",
                    142)
        });
        String report = crashGenerator.generateSmartBlameCrashReport(
                simulatedCrash,
                Map.of("com.simibubi.create", createId),
                Map.of(createId, "mods/forge/create-0.5.1.f.jar"));
        assertTrue(report.contains("(forge, create@0.5.1.f)"));
        assertTrue(report.contains("mods/forge/create-0.5.1.f.jar"));
        assertTrue(report.contains("LOADED MODULE TABLE"));

        VesselWatchdogThread watchdog = new VesselWatchdogThread();
        watchdog.heartbeatTick(10_000L);
        assertFalse(watchdog.isServerHanging(15_000L, 60_000L));
        assertTrue(watchdog.isServerHanging(80_000L, 60_000L));
        String threadDump = watchdog.captureDiagnosticThreadDump();
        assertNotNull(threadDump);
        assertTrue(threadDump.contains("VESSEL WATCHDOG THREAD DUMP"));

        VesselCliFlagsParser cliParser = new VesselCliFlagsParser();
        cliParser.parseArguments(new String[]{"--nogui", "--vessel-version=1.21.1"});
        assertTrue(cliParser.isNoGuiRequested());
        VesselServerLauncher launcher = new VesselServerLauncher(tempDir);
        assertEquals(0, launcher.executeBootSequence(new String[]{"--nogui"}));
        assertEquals(16, launcher.getCompletedBootSteps());
        assertEquals("1.21.4", launcher.getResolvedActiveMcVersion());
    }
}
