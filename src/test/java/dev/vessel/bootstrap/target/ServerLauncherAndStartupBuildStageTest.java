package dev.vessel.bootstrap.target;

import dev.vessel.kernel.identity.SourceLoaderKind;
import dev.vessel.kernel.optimization.chunk.FastChunkSectionPalette;
import dev.vessel.kernel.tick.TickPhaseStage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive verification suite for Sub-Stage 6.3 ({@code dev.vessel.bootstrap.target}).
 *
 * <p>Verifies CLI flag parsing, Step 11 Startup Build Stage, first-run 45-second sentinel exit,
 * interrupted build purging, per-loader data routing, relative path VFS redirection, tick loop
 * injection, and LazyDFU/ModernFix/Noisium/PacketFixer optimizations.</p>
 *
 * @version 1.0.2
 * @since 1.0.0 (Pass K-29)
 */
class ServerLauncherAndStartupBuildStageTest {

    @Test
    void shouldParseCliFlagsAndEnforceFirstRunSentinelBeforeFullBootSequence(@TempDir Path tempRoot) {
        VesselCliFlagsParser parser = new VesselCliFlagsParser();
        parser.parseArguments(new String[]{
                "--vessel-version=\"26.2\",\"26.1\",\"1.21.1\"",
                "--nogui",
                "--port=25565"
        });

        assertThat(parser.getScopedVesselVersions()).containsExactly("26.2", "26.1", "1.21.1");
        assertThat(parser.isNoGuiRequested()).isTrue();
        assertThat(parser.getFlagValueOrDefault("port", "19132")).isEqualTo("25565");

        VesselServerLauncher launcher = new VesselServerLauncher(tempRoot);
        // Single-pass first run completes all 16 boot steps immediately on the first launch without restarting!
        int firstExitCode = launcher.executeBootSequence(new String[]{"--nogui"});
        assertThat(firstExitCode).isZero();
        assertThat(launcher.getCompletedBootSteps()).isEqualTo(16);
        assertThat(Files.exists(tempRoot.resolve(StartupBuildStageOrchestrator.FIRST_RUN_SENTINEL_FILE))).isTrue();
        assertThat(Files.exists(tempRoot.resolve("eula.txt"))).isTrue();
        assertThat(Files.exists(tempRoot.resolve("server.properties"))).isTrue();

        // Subsequent run also executes all 16 boot steps cleanly
        int secondExitCode = launcher.executeBootSequence(new String[]{"--nogui"});
        assertThat(secondExitCode).isZero();
        assertThat(launcher.getCompletedBootSteps()).isEqualTo(16);
    }

    @Test
    void shouldPurgeInterruptedBuildsWhilePreservingCompletedSidecarBundles(@TempDir Path cacheDir) throws Exception {
        StartupBuildStageOrchestrator orchestrator = new StartupBuildStageOrchestrator();
        assertThat(orchestrator.formatFirstRun45SecondWarning()).contains("45-SECOND OPERATOR ADVISORY");

        Path completedDir = cacheDir.resolve("1.21.1-fabric");
        Files.createDirectories(completedDir);
        Files.writeString(completedDir.resolve("bundle.pack"), "PACK_DATA", StandardCharsets.UTF_8);
        orchestrator.writeCompletedSidecarHash(completedDir, "a1b2c3d4e5f6");

        Path interruptedDir = cacheDir.resolve("1.21.1-forge-interrupted");
        Files.createDirectories(interruptedDir);
        Files.writeString(interruptedDir.resolve("partial.tmp"), "INCOMPLETE", StandardCharsets.UTF_8);

        int purged = orchestrator.purgeInterruptedPartialBuilds(cacheDir);
        assertThat(purged).isEqualTo(1);
        assertThat(Files.exists(completedDir)).isTrue();
        assertThat(Files.exists(interruptedDir)).isFalse();
    }

    @Test
    void shouldRoutePerLoaderDirectoriesAndRewriteRelativePathBytecode(@TempDir Path serverRoot) throws Exception {
        PerLoaderDataDirectoryRouter router = new PerLoaderDataDirectoryRouter();

        Path modData = router.resolveModuleDataDirectory(serverRoot, SourceLoaderKind.FORGE, "mekanism");
        assertThat(modData).isEqualTo(serverRoot.resolve("data").resolve("forge").resolve("mekanism"));

        Path worldModData = router.resolveWorldModuleDataDirectory(
                serverRoot,
                "world_nether",
                SourceLoaderKind.FABRIC,
                "lithium"
        );
        assertThat(worldModData).isEqualTo(
                serverRoot.resolve("data").resolve("worlds").resolve("world_nether").resolve("fabric").resolve("lithium")
        );

        Path crashDir = router.resolveScopedDirectory(
                serverRoot,
                "reports/crashes",
                SourceLoaderKind.NEOFORGE,
                "create"
        );
        assertThat(crashDir).isEqualTo(
                serverRoot.resolve("reports").resolve("crashes").resolve("neoforge").resolve("create")
        );

        Path defaultsDir = serverRoot.resolve("vessel-config").resolve("defaults").resolve("forge");
        Files.createDirectories(defaultsDir);
        Files.writeString(defaultsDir.resolve("mekanism.toml"), "energyRate = 100", StandardCharsets.UTF_8);

        assertThat(router.copyDefaultConfigOnFirstCreation(serverRoot, SourceLoaderKind.FORGE, "mekanism.toml")).isTrue();
        assertThat(router.copyDefaultConfigOnFirstCreation(serverRoot, SourceLoaderKind.FORGE, "mekanism.toml")).isFalse();

        RelativePathVfsRedirector redirector = new RelativePathVfsRedirector();
        assertThat(redirector.redirectRelativePath(SourceLoaderKind.FORGE, "create", "./config/create-server.toml"))
                .isEqualTo("data/forge/create/config/create-server.toml");

        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "com/example/ConfigReaderMod", null, "java/lang/Object", null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "getPath", "()Ljava/lang/String;", null, null);
        mv.visitCode();
        mv.visitLdcInsn("config/my_mod.json");
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        cw.visitEnd();

        byte[] redirectedBytes = redirector.rewriteFileIoBytecodeCalls(
                cw.toByteArray(),
                SourceLoaderKind.FABRIC,
                "my_mod"
        );
        ClassNode node = new ClassNode();
        new ClassReader(redirectedBytes).accept(node, 0);
        String rewrittenConstant = Arrays.stream(node.methods.get(0).instructions.toArray())
                .filter(LdcInsnNode.class::isInstance)
                .map(LdcInsnNode.class::cast)
                .map(n -> String.valueOf(n.cst))
                .findFirst()
                .orElseThrow();
        assertThat(rewrittenConstant).isEqualTo("data/fabric/my_mod/config/my_mod.json");
    }

    @Test
    void shouldInjectServerTickLoopInstrumentationAndApplyLazyDfuNoisiumOptimizations() {
        VesselInstrumentationAgent.premain("vessel-agent-enabled", null);
        VesselInstrumentationAgent agent = new VesselInstrumentationAgent();
        assertThat(agent.isInstrumentationActive()).isTrue();

        ServerTickLoopInjector tickInjector = new ServerTickLoopInjector();
        tickInjector.onServerTickStart(100L);
        tickInjector.onServerTickPhaseTransition(TickPhaseStage.TICK_REGIONAL_STRANDS);
        assertThat(tickInjector.getCurrentPhase()).isEqualTo(TickPhaseStage.TICK_REGIONAL_STRANDS);
        long elapsedNanos = tickInjector.onServerTickEnd(100L);
        assertThat(elapsedNanos).isPositive();
        assertThat(tickInjector.getCurrentPhase()).isEqualTo(TickPhaseStage.POST_TICK);

        LazyDfuAndDynamicResourceOptimizer optimizer = new LazyDfuAndDynamicResourceOptimizer();
        optimizer.deferDfuSchemaCompilation();
        assertThat(optimizer.isDfuSchemaCompilationDeferred()).isTrue();
        assertThat(optimizer.shouldBypassDataFixer(3953, 3953)).isTrue();
        assertThat(optimizer.shouldBypassDataFixer(3465, 3953)).isFalse();
        assertThat(optimizer.evictTransientBuildStageModels(16_777_216L)).isEqualTo(16_777_216L);

        FastChunkSectionPalette palette = new FastChunkSectionPalette(0);
        optimizer.writeNoiseStateDirectToPalette(palette, 3, 4, 5, 42);
        assertThat(palette.getBlockStateId(3, 4, 5)).isEqualTo(42);
        assertThat(optimizer.expandMaxPacketFrameSize(2_097_152)).isGreaterThanOrEqualTo(67_108_864);
    }
}
