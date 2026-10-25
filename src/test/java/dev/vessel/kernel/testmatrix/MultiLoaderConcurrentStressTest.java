package dev.vessel.kernel.testmatrix;

import dev.vessel.kernel.identity.SourceLoaderKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Concurrency stress tests verifying multi-loader classpath isolation and extreme virtual bot load stability.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class MultiLoaderConcurrentStressTest {

    @Test
    @DisplayName("Verify concurrent bootstrap of Forge Create, Fabric Sodium, and Paper WorldEdit without collisions")
    public void testConcurrentMultiLoaderBootstrapWithoutCollision() throws Exception {
        MultiLoaderConcurrentTestHarness harness = new MultiLoaderConcurrentTestHarness();

        // 1. Mod Forge: Create
        MultiLoaderTestTarget forgeCreate = new MultiLoaderTestTarget(
                "create",
                SourceLoaderKind.FORGE,
                "0.5.1",
                Set.of("com.simibubi.create", "net.minecraftforge.fml"),
                Set.of("com.simibubi.create.CreateMod")
        );

        // 2. Mod Fabric: Sodium
        MultiLoaderTestTarget fabricSodium = new MultiLoaderTestTarget(
                "sodium",
                SourceLoaderKind.FABRIC,
                "0.5.8",
                Set.of("me.jellysquid.mods.sodium", "net.fabricmc.api"),
                Set.of("me.jellysquid.mods.sodium.client.SodiumClientMod")
        );

        // 3. Plugin Paper: WorldEdit
        MultiLoaderTestTarget paperWorldEdit = new MultiLoaderTestTarget(
                "worldedit",
                SourceLoaderKind.PAPER,
                "7.3.0",
                Set.of("com.sk89q.worldedit", "org.bukkit.plugin"),
                Set.of("com.sk89q.worldedit.bukkit.WorldEditPlugin")
        );

        harness.registerTarget(forgeCreate);
        harness.registerTarget(fabricSodium);
        harness.registerTarget(paperWorldEdit);

        assertThat(harness.getRegisteredTargets()).hasSize(3);

        MultiLoaderTestResult result = harness.executeConcurrentTest(5000L, true);

        assertThat(result.successful()).isTrue();
        assertThat(result.collisionCount()).isEqualTo(0);
        assertThat(result.conflictDetails()).isEmpty();
        assertThat(result.totalTargetsTested()).isEqualTo(3);
    }

    @Test
    @DisplayName("Verify strict mode detection and exception throwing on deliberate package collision")
    public void testCollisionDetectionThrowsException() {
        MultiLoaderConcurrentTestHarness harness = new MultiLoaderConcurrentTestHarness();

        MultiLoaderTestTarget moduleA = new MultiLoaderTestTarget(
                "module-a",
                SourceLoaderKind.FORGE,
                "1.0.0",
                Set.of("com.vessel.shared.conflict"),
                Set.of("com.vessel.shared.conflict.EntryA")
        );

        MultiLoaderTestTarget moduleB = new MultiLoaderTestTarget(
                "module-b",
                SourceLoaderKind.FABRIC,
                "1.0.0",
                Set.of("com.vessel.shared.conflict"),
                Set.of("com.vessel.shared.conflict.EntryB")
        );

        harness.registerTarget(moduleA);
        harness.registerTarget(moduleB);

        assertThatThrownBy(() -> harness.executeConcurrentTest(5000L, true))
                .isInstanceOf(ClasspathCollisionException.class)
                .hasMessageContaining("com.vessel.shared.conflict");
    }

    @Test
    @DisplayName("Execute extreme bot stress load runner with 100 bots and confirm zero dropped packets")
    public void testConcurrentModStressLoadRunner() {
        ConcurrentModStressLoadRunner runner = new ConcurrentModStressLoadRunner();
        VirtualBotConfig config = new VirtualBotConfig(100, 20, 5, 0.25, 20.0);

        ModStressLoadReport report = runner.runStressTest(config);

        assertThat(report.totalBots()).isEqualTo(100);
        assertThat(report.totalPacketsSent()).isEqualTo(100L * 20L * 5L);
        assertThat(report.totalPacketsProcessed()).isEqualTo(report.totalPacketsSent());
        assertThat(report.packetsDropped()).isEqualTo(0L);
        assertThat(report.errorCount()).isEqualTo(0);
        assertThat(report.isStabilityCompliant()).isTrue();
    }
}
