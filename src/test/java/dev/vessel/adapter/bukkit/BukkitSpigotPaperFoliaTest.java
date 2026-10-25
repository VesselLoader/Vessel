package dev.vessel.adapter.bukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vessel.command.BuiltinSamplerProfiler;
import dev.vessel.command.ParallelChunkPregenerator;
import dev.vessel.command.VesselRootCommand;
import dev.vessel.kernel.actor.GlobalRegionActorStrand;
import dev.vessel.kernel.actor.RegionCoordinateHash;
import dev.vessel.kernel.actor.RegionalActorStrand;
import dev.vessel.kernel.actor.primitive.RegionStrandRouter;
import dev.vessel.kernel.config.yaml.MemoryConfigurationSection;
import dev.vessel.kernel.event.EventExecutionPhase;
import dev.vessel.kernel.identity.SourceLoaderKind;
import dev.vessel.kernel.identity.VesselModuleIdentifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Sub-Stage 6.13: Bukkit, Spigot, Paper, and Folia shims plus the built-in
 * chunk pregenerator, sampler profiler, and 13 {@code /vessel} subcommands.
 *
 * @version 1.0.1
 */
class BukkitSpigotPaperFoliaTest {

    @Test
    void testBukkitSpigotAndPaperBootstrapAdapters() {
        BukkitPluginBootstrapAdapter bukkitAdapter = new BukkitPluginBootstrapAdapter();
        VesselModuleIdentifier bukkitId = bukkitAdapter.loadLegacyBukkitPlugin(
                "name: WorldGuard\nversion: 7.0.9\nmain: com.sk89q.worldguard.bukkit.WorldGuardPlugin\n");
        assertEquals(SourceLoaderKind.BUKKIT, bukkitId.loader());
        assertEquals("worldguard", bukkitId.namespace());
        assertEquals("7.0.9", bukkitId.version());
        MemoryConfigurationSection cfg = bukkitAdapter.getPluginConfig("WorldGuard");
        assertEquals("7.0.9", cfg.getString("plugin.version"));

        SpigotPluginAdapter spigotAdapter = new SpigotPluginAdapter();
        assertEquals(
                "<green><bold>VESSEL Ready",
                spigotAdapter.convertBungeeComponentToMiniMessage("§a§lVESSEL Ready"));
        UUID playerUuid = UUID.randomUUID();
        spigotAdapter.sendActionBarViaSpigotShim(playerUuid, "&eMana: 100/100");
        assertEquals("<yellow>Mana: 100/100", spigotAdapter.getLastActionBarMessage(playerUuid));

        PaperPluginBootstrapAdapter paperAdapter = new PaperPluginBootstrapAdapter();
        String paperYaml = """
                name: EssentialsX-Paper
                version: 2.21.0
                folia-supported: true
                libraries:
                  - net.kyori:adventure-text-minimessage:4.17.0
                  - com.zaxxer:HikariCP:5.1.0
                """;
        VesselModuleIdentifier paperId = paperAdapter.parsePaperPluginYaml(paperYaml);
        assertEquals(SourceLoaderKind.FOLIA, paperId.loader());
        assertEquals("essentialsx-paper", paperId.namespace());
        assertEquals(
                List.of(
                        "net.kyori:adventure-text-minimessage:4.17.0",
                        "com.zaxxer:HikariCP:5.1.0"),
                paperAdapter.extractPaperLibraryCoordinates(paperYaml));
    }

    @Test
    void testFoliaSchedulerPaperPluginManagerAndUnifiedPermissionBridge() throws Exception {
        RegionStrandRouter router = new RegionStrandRouter();
        try (RegionalActorStrand regionStrand =
                        new RegionalActorStrand(new RegionCoordinateHash(0, 0));
                GlobalRegionActorStrand globalStrand = new GlobalRegionActorStrand()) {
            router.register(regionStrand);
            FoliaSchedulerShim foliaShim = new FoliaSchedulerShim();
            CountDownLatch latch = new CountDownLatch(3);

            assertTrue(foliaShim.scheduleRegionTask(2, 3, latch::countDown, router));
            assertTrue(foliaShim.scheduleGlobalRegionTask(latch::countDown, globalStrand));
            assertTrue(foliaShim.scheduleEntityTask(
                    UUID.randomUUID(), 1, 1, latch::countDown, router));
            assertTrue(latch.await(3, TimeUnit.SECONDS));
        }

        PaperPluginManagerShim pluginManager = new PaperPluginManagerShim();
        List<String> order = new ArrayList<>();
        pluginManager.registerPluginListener(
                "Vault", "BlockBreakEvent", EventExecutionPhase.LATE, ev -> order.add("LATE"));
        pluginManager.registerPluginListener(
                "WorldGuard", "BlockBreakEvent", EventExecutionPhase.EARLIEST, ev -> order.add("EARLIEST"));
        assertTrue(pluginManager.isPluginEnabled("Vault"));
        assertTrue(pluginManager.callBukkitEvent("BlockBreakEvent", new Object()));
        assertEquals(List.of("EARLIEST", "LATE"), order);

        VesselPermissionBridge permBridge = new VesselPermissionBridge();
        UUID adminUuid = UUID.randomUUID();
        permBridge.grantPermission(adminUuid, "vessel.command.*");
        assertTrue(permBridge.hasUnifiedPermission(adminUuid, "vessel.command.tps"));
        assertFalse(permBridge.hasUnifiedPermission(adminUuid, "luckperms.editor"));
    }

    @Test
    void testParallelChunkPregeneratorSamplerProfilerAndAll13VesselSubcommands() {
        ParallelChunkPregenerator pregenerator = new ParallelChunkPregenerator();
        List<Long> ringChunks = pregenerator.startRingPregeneration("world", 0, 0, 1);
        assertEquals(9, ringChunks.size());
        assertEquals(100.0, pregenerator.getPregenerationProgressPercent("world"), 1e-4);

        BuiltinSamplerProfiler profiler = new BuiltinSamplerProfiler();
        profiler.startSamplingSession();
        profiler.recordMethodSample("forge:create#KineticNetwork.tick", 400_000L);
        profiler.recordMethodSample("fabric:lithium#WorldTick.step", 150_000L);
        profiler.recordMethodSample("forge:create#KineticNetwork.tick", 200_000L);
        Map<String, Long> summary = profiler.stopAndExportFlamegraphSummary();
        assertEquals(600_000L, summary.get("forge:create#KineticNetwork.tick"));
        assertEquals(150_000L, summary.get("fabric:lithium#WorldTick.step"));

        VesselRootCommand rootCommand = new VesselRootCommand();
        List<String> all13 = List.of(
                "version", "tps", "mods", "plugins", "top", "profile", "heap",
                "chunks", "entities", "pregen", "status", "reload", "dump");
        for (String sub : all13) {
            String output = rootCommand.executeCommand(new String[]{sub});
            assertTrue(output.startsWith("[VESSEL]"), "Failed output for subcommand: " + sub);
            assertFalse(output.contains("Unknown subcommand"));
        }
        assertEquals(List.of("plugins", "profile", "pregen"), rootCommand.suggestTabCompletions("p"));
    }
}
