package dev.vessel.bootstrap.mojang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vessel.bootstrap.target.VesselCliFlagsParser;
import dev.vessel.bootstrap.target.VesselServerLauncher;
import dev.vessel.kernel.identity.SourceLoaderKind;
import dev.vessel.kernel.identity.VesselModuleIdentifier;
import java.io.ByteArrayOutputStream;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Unit tests for Sub-Stage 7.1: VESSEL custom flags suite (`--vessel-deep-debug`,
 * `--vessel-ultra-optimize`, `--vessel-single-pass-boot`), Mojang `latest.release` version
 * resolution, bundler extraction, EULA/server.properties bootstrap, real directory JAR scanning,
 * and Tier-1 Vanilla main class invocation.
 *
 * @version 1.0.0
 */
class MojangBundlerAndAutoLatestResolverTest {

    @Test
    void testCustomFlagsSuiteDeepDebugUltraOptimizeAndSinglePassDefaults() {
        VesselCliFlagsParser parser = new VesselCliFlagsParser();
        parser.parseArguments(new String[]{
            "--nogui",
            "--vessel-deep-debug",
            "--vessel-ultra-optimize"
        });

        VesselCustomFlagsProfile profile = parser.getCustomFlagsProfile();
        assertTrue(profile.isDeepDebugEnabled());
        assertTrue(profile.isFlagEnabled(VesselCustomFlagsProfile.FLAG_TRACE_BYTECODE));
        assertTrue(profile.isFlagEnabled(VesselCustomFlagsProfile.FLAG_TRACE_CLASSLOADER));
        assertTrue(profile.isFlagEnabled(VesselCustomFlagsProfile.FLAG_TRACE_EVENTS));
        assertTrue(profile.isFlagEnabled(VesselCustomFlagsProfile.FLAG_TRACE_REGISTRY));
        assertTrue(profile.isFlagEnabled(VesselCustomFlagsProfile.FLAG_PROFILE_BOOT));

        assertTrue(profile.isUltraOptimizationEnabled());
        assertTrue(profile.isFlagEnabled(VesselCustomFlagsProfile.FLAG_SIMD_PHYSICS));
        assertTrue(profile.isFlagEnabled(VesselCustomFlagsProfile.FLAG_ZERO_COPY_NETTY));
        assertTrue(profile.isFlagEnabled(VesselCustomFlagsProfile.FLAG_AGGRESSIVE_AI_LOD));
        assertTrue(profile.isFlagEnabled(VesselCustomFlagsProfile.FLAG_ZSTD_RAM_CACHE));
        assertTrue(profile.isFlagEnabled(VesselCustomFlagsProfile.FLAG_LOCKLESS_STRANDS));
        assertTrue(profile.isFlagEnabled(VesselCustomFlagsProfile.FLAG_LAZY_DFU_NOISIUM));

        assertTrue(profile.isSinglePassBootEnabled());
        assertTrue(profile.isUniversalCompatEnabled());
        assertTrue(profile.isAutoLatestVersionEnabled());
        assertTrue(profile.isLenientLoadEnabled());
    }

    @Test
    void testMojangManifestLatestReleaseDownloaderBundlerExtractorAndTierOneLaunch(
            @TempDir Path tempDir) throws Exception {
        MojangVersionManifestResolver resolver = new MojangVersionManifestResolver();
        String manifestJson = resolver.fetchOrLoadCachedVersionManifest(tempDir, "offline://cached");
        String latestRelease = resolver.resolveLatestReleaseVersion(manifestJson);
        assertEquals("1.21.4", latestRelease);
        assertEquals(
                "https://piston-meta.mojang.com/v1/packages/1.21.4/1.21.4.json",
                resolver.extractVersionMetadataUrl(manifestJson, "1.21.4"));

        byte[] innerServerJarBytes = createSyntheticExecutableServerJar();
        Path mockBundlerSource = tempDir.resolve("mock-bundler.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(mockBundlerSource))) {
            writeJarEntry(
                    jos,
                    "META-INF/main-class",
                    "net.minecraft.server.Main\n".getBytes(StandardCharsets.UTF_8));
            writeJarEntry(
                    jos,
                    "META-INF/versions.list",
                    "hash123\t1.21.4\t1.21.4/server-1.21.4.jar\n".getBytes(StandardCharsets.UTF_8));
            writeJarEntry(
                    jos,
                    "META-INF/versions/1.21.4/server-1.21.4.jar",
                    innerServerJarBytes);
        }

        MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
        String expectedSha1 = HexFormat.of().formatHex(
                sha1Digest.digest(Files.readAllBytes(mockBundlerSource)));

        MojangServerJarDownloader downloader = new MojangServerJarDownloader();
        String sampleVersionJson = """
                {
                  "downloads": {
                    "server": {
                      "sha1": "%s",
                      "url": "%s"
                    }
                  }
                }
                """.formatted(expectedSha1, mockBundlerSource.toUri().toString());
        assertEquals(expectedSha1, downloader.extractServerDownloadSha1(sampleVersionJson));
        Path cachedJar = downloader.ensureVanillaServerJarCached(
                tempDir.resolve("cache"),
                "1.21.4",
                downloader.extractServerDownloadUrl(sampleVersionJson),
                expectedSha1);
        assertTrue(downloader.verifyFileSha1(cachedJar, expectedSha1));

        MojangBundlerExtractor extractor = new MojangBundlerExtractor();
        assertEquals("net.minecraft.server.Main", extractor.readBundlerMainClass(cachedJar));
        List<Path> extractedClasspath = extractor.extractBundlerJar(
                cachedJar, tempDir.resolve("cache").resolve("bundler"));
        assertEquals(1, extractedClasspath.size());

        TierOneVanillaGameLauncher tierOneLauncher = new TierOneVanillaGameLauncher();
        try (URLClassLoader gameLoader = tierOneLauncher.createTierOneGameClassLoader(
                extractedClasspath, ClassLoader.getSystemClassLoader())) {
            assertTrue(tierOneLauncher.invokeMinecraftServerMain(
                    gameLoader,
                    "net.minecraft.server.Main",
                    new String[]{"--nogui"}));
            assertEquals("BOOTED_1.21.4", System.getProperty("vessel.test.mc.main.booted"));
        }
    }

    @Test
    void testSinglePassFirstRunWithRealModAndPluginDirectoryScanning(@TempDir Path serverRoot)
            throws Exception {
        Path fabricModJar = serverRoot.resolve("mods").resolve("fabric").resolve("lithium-0.14.0.jar");
        Files.createDirectories(fabricModJar.getParent());
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(fabricModJar))) {
            writeJarEntry(
                    jos,
                    "fabric.mod.json",
                    "{\"id\":\"lithium\",\"version\":\"0.14.0\"}".getBytes(StandardCharsets.UTF_8));
        }

        Path paperPluginJar = serverRoot.resolve("plugins").resolve("WorldEdit-7.3.0.jar");
        Files.createDirectories(paperPluginJar.getParent());
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(paperPluginJar))) {
            writeJarEntry(
                    jos,
                    "plugin.yml",
                    "name: WorldEdit\nversion: 7.3.0\n".getBytes(StandardCharsets.UTF_8));
        }

        Path unlabeledModJar = serverRoot.resolve("mods").resolve("forge").resolve("jei-19.21.0.jar");
        Files.createDirectories(unlabeledModJar.getParent());
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(unlabeledModJar))) {
            writeJarEntry(jos, "README.txt", "JEI".getBytes(StandardCharsets.UTF_8));
        }

        VesselServerLauncher launcher = new VesselServerLauncher(serverRoot);
        int code = launcher.executeBootSequence(new String[]{
            "--nogui",
            "--vessel-ultra-optimize",
            "--vessel-deep-debug"
        });
        assertEquals(0, code);
        assertEquals(16, launcher.getCompletedBootSteps());
        assertEquals("1.21.4", launcher.getResolvedActiveMcVersion());

        Map<Path, VesselModuleIdentifier> discovered = launcher.getDiscoveredModules();
        assertEquals(3, discovered.size());
        assertEquals(
                VesselModuleIdentifier.of(SourceLoaderKind.FABRIC, "lithium", "0.14.0"),
                discovered.get(fabricModJar));
        assertEquals(
                VesselModuleIdentifier.of(SourceLoaderKind.BUKKIT, "worldedit", "7.3.0"),
                discovered.get(paperPluginJar));
        assertEquals(
                VesselModuleIdentifier.of(SourceLoaderKind.FORGE, "jei", "19.21.0"),
                discovered.get(unlabeledModJar));

        ServerEulaAndPropertiesBootstrap eulaBootstrap = new ServerEulaAndPropertiesBootstrap();
        assertTrue(eulaBootstrap.ensureEulaAccepted(serverRoot, false));
        assertFalse(eulaBootstrap.ensureEulaAccepted(serverRoot.resolve("unaccepted"), false));
    }

    private static byte[] createSyntheticExecutableServerJar() throws Exception {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(
                Opcodes.V21,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                "net/minecraft/server/Main",
                null,
                "java/lang/Object",
                null);
        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "main",
                "([Ljava/lang/String;)V",
                null,
                null);
        mv.visitCode();
        mv.visitLdcInsn("vessel.test.mc.main.booted");
        mv.visitLdcInsn("BOOTED_1.21.4");
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/lang/System",
                "setProperty",
                "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                false);
        mv.visitInsn(Opcodes.POP);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();
        cw.visitEnd();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JarOutputStream jos = new JarOutputStream(baos)) {
            writeJarEntry(jos, "net/minecraft/server/Main.class", cw.toByteArray());
        }
        return baos.toByteArray();
    }

    private static void writeJarEntry(JarOutputStream jos, String name, byte[] bytes)
            throws Exception {
        JarEntry entry = new JarEntry(name);
        jos.putNextEntry(entry);
        jos.write(bytes);
        jos.closeEntry();
    }
}
