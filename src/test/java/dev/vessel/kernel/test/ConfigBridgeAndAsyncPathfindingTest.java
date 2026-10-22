package dev.vessel.kernel.test;

import dev.vessel.kernel.config.HierarchicalConfigNode;
import dev.vessel.kernel.config.bridge.ConfigKeyMapping;
import dev.vessel.kernel.config.bridge.CrossLoaderConfigBridge;
import dev.vessel.kernel.config.yaml.ConfigurationSection;
import dev.vessel.kernel.config.yaml.SpigotYamlConfigurationAdapter;
import dev.vessel.kernel.identity.SourceLoaderKind;
import dev.vessel.kernel.migration.MultiWorldStorageLayoutMigrator;
import dev.vessel.kernel.migration.WorldDimensionType;
import dev.vessel.kernel.migration.WorldStorageDirectoryLayout;
import dev.vessel.kernel.optimization.entity.AsyncPathfindingDispatcher;
import dev.vessel.kernel.optimization.entity.PathfindingGoal;
import dev.vessel.kernel.optimization.entity.PathfindingResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification test suite for Pass K-20:
 * Spigot YamlConfiguration Adapter, Cross-Loader Config Bridge,
 * Async Pathfinding Dispatcher, and Multi-World Storage Layout Migrator.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class ConfigBridgeAndAsyncPathfindingTest {

    // =========================================================================
    // 1. SpigotYamlConfigurationAdapter Tests
    // =========================================================================

    @Test
    @DisplayName("YamlAdapter: Parses hierarchy, preserves comments, lists, and translates color codes")
    void testYamlAdapterBasics() {
        SpigotYamlConfigurationAdapter adapter = new SpigotYamlConfigurationAdapter();

        String rawYaml = """
                # Server Main Configuration
                server:
                  # Port to listen on
                  port: 25565
                  motd: '&aWelcome to &bVESSEL &cServer!'
                  enabled: true
                  max-memory: 4096
                  ratio: 1.75
                modules:
                  - 'vessel-core'
                  - 'vessel-render'
                """;

        adapter.loadFromString(rawYaml);

        assertThat(adapter.getInt("server.port")).isEqualTo(25565);
        assertThat(adapter.getBoolean("server.enabled")).isTrue();
        assertThat(adapter.getLong("server.max-memory")).isEqualTo(4096L);
        assertThat(adapter.getDouble("server.ratio")).isEqualTo(1.75);

        String motd = adapter.getString("server.motd");
        assertThat(motd).isEqualTo("&aWelcome to &bVESSEL &cServer!");

        // Color code translation
        String coloredMotd = SpigotYamlConfigurationAdapter.translateAlternateColorCodes('&', motd);
        assertThat(coloredMotd).isEqualTo("§aWelcome to §bVESSEL §cServer!");
        assertThat(SpigotYamlConfigurationAdapter.stripColor(coloredMotd)).isEqualTo("Welcome to VESSEL Server!");

        // List parsing
        List<String> modules = adapter.getStringList("modules");
        assertThat(modules).containsExactly("vessel-core", "vessel-render");

        // Section access
        ConfigurationSection serverSec = adapter.getConfigurationSection("server");
        assertThat(serverSec).isNotNull();
        assertThat(serverSec.getInt("port")).isEqualTo(25565);

        // Serialization and comment preservation
        String serialized = adapter.saveToString();
        assertThat(serialized).contains("# Port to listen on");
        assertThat(serialized).contains("port: 25565");

        // Conversion to HierarchicalConfigNode
        HierarchicalConfigNode node = adapter.toConfigNode();
        assertThat(node.getNode("server.port").getValue()).isEqualTo(25565);
    }

    // =========================================================================
    // 2. CrossLoaderConfigBridge Tests
    // =========================================================================

    @Test
    @DisplayName("CrossLoaderConfigBridge: Synchronizes and translates config trees across Forge, Fabric, and Bukkit")
    void testCrossLoaderConfigBridge() {
        CrossLoaderConfigBridge bridge = new CrossLoaderConfigBridge();

        // Check pre-registered mappings
        String forgePort = bridge.mapKey(SourceLoaderKind.BUKKIT, "server-port", SourceLoaderKind.FORGE);
        assertThat(forgePort).isEqualTo("server.listen_port");

        String fabricPort = bridge.mapKey(SourceLoaderKind.BUKKIT, "server-port", SourceLoaderKind.FABRIC);
        assertThat(fabricPort).isEqualTo("serverPort");

        String canonical = bridge.resolveCanonicalKey(SourceLoaderKind.BUKKIT, "view-distance");
        assertThat(canonical).isEqualTo("world.view_distance");

        // Translate tree from Bukkit to Forge
        HierarchicalConfigNode bukkitTree = new HierarchicalConfigNode("");
        bukkitTree.getOrCreateNode("server-port").setValue(25565);
        bukkitTree.getOrCreateNode("view-distance").setValue(12);

        HierarchicalConfigNode forgeTree = bridge.translateTree(bukkitTree, SourceLoaderKind.BUKKIT, SourceLoaderKind.FORGE);
        assertThat(forgeTree.getNode("server.listen_port").getValue()).isEqualTo(25565);
        assertThat(forgeTree.getNode("world.view_distance").getValue()).isEqualTo(12);

        // Custom mapping registration
        bridge.registerMapping(new ConfigKeyMapping(
                "custom.tps_limit",
                "engine.max_tps",
                "maxTps",
                "tps-cap",
                20
        ));
        assertThat(bridge.mapKey(SourceLoaderKind.SPIGOT, "tps-cap", SourceLoaderKind.FABRIC)).isEqualTo("maxTps");
    }

    // =========================================================================
    // 3. AsyncPathfindingDispatcher Tests
    // =========================================================================

    @Test
    @DisplayName("AsyncPathfindingDispatcher: Computes paths asynchronously and cancels stale superseded requests")
    void testAsyncPathfindingExecutionAndCancellation() throws InterruptedException {
        AsyncPathfindingDispatcher dispatcher = new AsyncPathfindingDispatcher(2, 256);

        // 1. Submit valid path request
        PathfindingGoal goal = new PathfindingGoal(100, 0.0, 64.0, 0.0, 10.0, 64.0, 0.0, 32.0, 1.0f);
        CountDownLatch latch1 = new CountDownLatch(1);
        AtomicReference<PathfindingResult> resultRef = new AtomicReference<>();

        dispatcher.submitPathRequest(goal, res -> {
            resultRef.set(res);
            latch1.countDown();
        });

        boolean finished = latch1.await(2, TimeUnit.SECONDS);
        assertThat(finished).isTrue();

        PathfindingResult result = resultRef.get();
        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.cancelled()).isFalse();
        assertThat(result.waypointCount()).isGreaterThan(0);
        assertThat(result.waypointsX()[result.waypointCount() - 1]).isEqualTo(10.0);

        // 2. Test cancellation of stale request
        CountDownLatch latchCancel = new CountDownLatch(1);
        AtomicReference<PathfindingResult> cancelRef = new AtomicReference<>();

        // Submit first request
        dispatcher.submitPathRequest(new PathfindingGoal(200, 0, 64, 0, 50, 64, 0, 100, 1.0f), res -> {
            if (res.cancelled()) {
                cancelRef.set(res);
                latchCancel.countDown();
            }
        });

        // Immediately cancel by updating entity target
        dispatcher.cancelEntityRequests(200);

        boolean cancelFinished = latchCancel.await(2, TimeUnit.SECONDS);
        assertThat(cancelFinished).isTrue();
        assertThat(cancelRef.get().cancelled()).isTrue();

        dispatcher.shutdown();
        boolean terminated = dispatcher.awaitTermination(2, TimeUnit.SECONDS);
        assertThat(terminated).isTrue();
    }

    // =========================================================================
    // 4. MultiWorldStorageLayoutMigrator Tests
    // =========================================================================

    @Test
    @DisplayName("MultiWorldStorageLayoutMigrator: Detects directory layout, plans migration, and resolves dimension paths")
    void testMultiWorldStorageLayoutMigration() {
        MultiWorldStorageLayoutMigrator migrator = new MultiWorldStorageLayoutMigrator();

        // 1. Detect layout via file predicate
        Map<String, Boolean> spigotFileSystem = new HashMap<>();
        spigotFileSystem.put("world", true);
        spigotFileSystem.put("world_nether", true);
        spigotFileSystem.put("world_the_end", true);

        WorldStorageDirectoryLayout spigotLayout = migrator.detectLayout(spigotFileSystem::containsKey, "world");
        assertThat(spigotLayout).isEqualTo(WorldStorageDirectoryLayout.SPIGOT_SPLIT);

        Map<String, Boolean> vanillaFileSystem = new HashMap<>();
        vanillaFileSystem.put("world", true);
        vanillaFileSystem.put("world/DIM-1", true);
        vanillaFileSystem.put("world/level.dat", true);

        WorldStorageDirectoryLayout vanillaLayout = migrator.detectLayout(vanillaFileSystem::containsKey, "world");
        assertThat(vanillaLayout).isEqualTo(WorldStorageDirectoryLayout.VANILLA_UNIFIED);

        // 2. Resolve dimension directories
        String netherSpigot = migrator.resolveDimensionDirectory(
                WorldStorageDirectoryLayout.SPIGOT_SPLIT, "world", WorldDimensionType.THE_NETHER
        );
        assertThat(netherSpigot).isEqualTo("world_nether");

        String netherVanilla = migrator.resolveDimensionDirectory(
                WorldStorageDirectoryLayout.VANILLA_UNIFIED, "world", WorldDimensionType.THE_NETHER
        );
        assertThat(netherVanilla).isEqualTo("world/DIM-1");

        // 3. Resolve region directories
        String netherRegionSpigot = migrator.resolveDimensionRegionDirectory(
                WorldStorageDirectoryLayout.SPIGOT_SPLIT, "world", WorldDimensionType.THE_NETHER
        );
        assertThat(netherRegionSpigot).isEqualTo("world_nether/DIM-1/region");

        // 4. Plan migration from Vanilla Unified to Spigot Split
        List<String> plan = migrator.planMigration(
                WorldStorageDirectoryLayout.VANILLA_UNIFIED,
                WorldStorageDirectoryLayout.SPIGOT_SPLIT,
                "world"
        );
        assertThat(plan).isNotEmpty();
        assertThat(plan).contains("MKDIR world_nether");
        assertThat(plan).contains("MOVE world/DIM-1 -> world_nether/DIM-1");
        assertThat(plan).contains("MKDIR world_the_end");
        assertThat(plan).contains("MOVE world/DIM1 -> world_the_end/DIM1");

        // Preferred layout
        assertThat(migrator.isSplitLayoutPreferred(SourceLoaderKind.PAPER)).isTrue();
        assertThat(migrator.isSplitLayoutPreferred(SourceLoaderKind.FABRIC)).isFalse();
    }
}
