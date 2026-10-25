package dev.vessel.command;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vessel.bootstrap.target.VesselUniversalResourcePackCompiler;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit verification suite for Universal Mod item aggregation, Universal Presentation
 * ResourcePack compilation from mod JARs, and Bukkit / EssentialsX home command bridging.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public final class VesselUniversalModAndCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCompileResourcePackFromDiscoveredModJarsWithValidFormat() throws Exception {
        Path serverRoot = tempDir.resolve("pack-server");
        Path modsDir = serverRoot.resolve("mods");
        Files.createDirectories(modsDir);

        // Synthesize a mock Create mod JAR with textures and item models
        Path createMockJar = modsDir.resolve("create-test.jar");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JarOutputStream jos = new JarOutputStream(baos)) {
            jos.putNextEntry(new JarEntry("assets/create/models/item/cogwheel.json"));
            jos.write("{\"parent\":\"item/generated\"}".getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();

            jos.putNextEntry(new JarEntry("assets/create/textures/item/cogwheel.png"));
            jos.write(new byte[]{1, 2, 3, 4});
            jos.closeEntry();
        }
        Files.write(createMockJar, baos.toByteArray());

        Path packZip = serverRoot.resolve("cache").resolve("ecosystem").resolve("vessel-pack.zip");
        VesselUniversalResourcePackCompiler compiler = new VesselUniversalResourcePackCompiler();
        String sha1 = compiler.compileResourcePack(serverRoot, packZip);

        assertThat(sha1).hasSize(40);
        assertThat(Files.isRegularFile(packZip)).isTrue();

        try (ZipFile zip = new ZipFile(packZip.toFile())) {
            assertThat(zip.getEntry("pack.mcmeta")).isNotNull();
            assertThat(zip.getEntry("assets/vessel/textures/item/vessel_core.png")).isNotNull();
            assertThat(zip.getEntry("assets/create/models/item/cogwheel.json")).isNotNull();
            assertThat(zip.getEntry("assets/create/textures/item/cogwheel.png")).isNotNull();
        }
    }

    @Test
    void shouldHandleBukkitHomeCommandsAndPersistenceCleanly() {
        Path serverRoot = tempDir.resolve("home-server");
        VesselBukkitCommandBridge bridge = new VesselBukkitCommandBridge();
        bridge.initialize(serverRoot);

        // Console attempts /sethome -> returns friendly error message
        List<String> consoleSetHome = bridge.executeBukkitCommand("sethome", new String[]{"base"}, null);
        assertThat(consoleSetHome).anyMatch(l -> l.contains("must be executed by an in-game player"));

        // Direct home store operations
        VesselHomeDataStore store = new VesselHomeDataStore();
        store.initialize(serverRoot);
        store.setHome("TheRealShiroz", "base", "minecraft:overworld", 100.5, 65.0, -200.5, 90.0f, 0.0f);

        String record = store.getHome("TheRealShiroz", "base");
        assertThat(record).isNotNull();
        assertThat(record).contains("minecraft:overworld:100.50:65.00:-200.50:90.00:0.00");

        List<String> homes = store.listHomes("TheRealShiroz");
        assertThat(homes).containsExactly("base");

        boolean deleted = store.deleteHome("TheRealShiroz", "base");
        assertThat(deleted).isTrue();
        assertThat(store.getHome("TheRealShiroz", "base")).isNull();
    }

    @Test
    void shouldInterceptBukkitCommandsInConsoleStream() {
        assertThat(dev.vessel.bootstrap.target.VesselConsoleCommandInterceptorStream.isVesselInterceptedLine("sethome")).isTrue();
        assertThat(dev.vessel.bootstrap.target.VesselConsoleCommandInterceptorStream.isVesselInterceptedLine("/sethome base")).isTrue();
        assertThat(dev.vessel.bootstrap.target.VesselConsoleCommandInterceptorStream.isVesselInterceptedLine("home")).isTrue();
        assertThat(dev.vessel.bootstrap.target.VesselConsoleCommandInterceptorStream.isVesselInterceptedLine("delhome")).isTrue();
        assertThat(dev.vessel.bootstrap.target.VesselConsoleCommandInterceptorStream.isVesselInterceptedLine("spawn")).isTrue();
        assertThat(dev.vessel.bootstrap.target.VesselConsoleCommandInterceptorStream.isVesselInterceptedLine("heal")).isTrue();
        assertThat(dev.vessel.bootstrap.target.VesselConsoleCommandInterceptorStream.isVesselInterceptedLine("feed")).isTrue();
        assertThat(dev.vessel.bootstrap.target.VesselConsoleCommandInterceptorStream.isVesselInterceptedLine("fly")).isTrue();
        assertThat(dev.vessel.bootstrap.target.VesselConsoleCommandInterceptorStream.isVesselInterceptedLine("restart")).isTrue();
        assertThat(dev.vessel.bootstrap.target.VesselConsoleCommandInterceptorStream.isVesselInterceptedLine("say hello")).isFalse();
    }

    @Test
    void shouldInitializeVesselOptimizationCoordinatorAndEngines() {
        Path serverRoot = tempDir.resolve("opt-server");
        dev.vessel.kernel.optimization.VesselOptimizationCoordinator coordinator =
                new dev.vessel.kernel.optimization.VesselOptimizationCoordinator();
        boolean ok = coordinator.initializeAllEngines(serverRoot);
        assertThat(ok).isTrue();
        assertThat(dev.vessel.kernel.optimization.VesselOptimizationCoordinator.areEnginesInitialized()).isTrue();
        assertThat(coordinator.getChunkThrottle()).isNotNull();
        assertThat(coordinator.getEntityLod()).isNotNull();
        assertThat(coordinator.getBrainThrottle()).isNotNull();
        assertThat(coordinator.getLightEngine()).isNotNull();
        assertThat(coordinator.getPaletteOptimizer()).isNotNull();
        assertThat(coordinator.getLeakSanitizer()).isNotNull();
        assertThat(System.getProperty("io.netty.allocator.type")).isEqualTo("pooled");
    }
}
