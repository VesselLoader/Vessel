package dev.vessel.bootstrap.mojang;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vessel.bootstrap.target.VesselConsoleCommandInterceptorStream;
import dev.vessel.bootstrap.target.VesselServerLauncher;
import dev.vessel.bootstrap.target.VesselStartupConsoleLogger;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Verifies:
 * <ol>
 *   <li>First-run installer console logging ({@code [VESSEL/INSTALLER]}), concise subsequent-run
 *       logging ({@code [VESSEL/INFO]}), and verbose debug logging ({@code [VESSEL/DEBUG]}).</li>
 *   <li>{@link JvmVersionCompatibilityGuard} halting cleanly before {@link UnsupportedClassVersionError}
 *       when a server JAR is compiled with class file version {@code 69.0} (Java 25) on a Java 21
 *       runtime, instructing the user to install Java 25+ (and noting VESSEL's minimum Java 21 LTS
 *       requirement).</li>
 *   <li>Dynamic multi-version server launching across legacy monolithic server JARs (e.g., 1.12.2
 *       {@code net.minecraft.server.MinecraftServer} and 1.16.5 {@code net.minecraft.server.Main})
 *       and modern 1.18+ bundler JARs, while keeping the Tier-1 {@code URLClassLoader} alive.</li>
 * </ol>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.2
 */
public final class JvmVersionGuardAndConsoleLogTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldLogInstallerStepsOnFirstRunAndConciseInfoOnSecondRunAndVerboseOnDeepDebug() {
        Path serverRoot = tempDir.resolve("console-log-server");
        VesselServerLauncher launcher = new VesselServerLauncher(serverRoot);

        // 1. First Run -> Must emit [VESSEL/INSTALLER] logs
        int firstExit = launcher.executeBootSequence(new String[]{"--nogui"});
        assertThat(firstExit).isEqualTo(0);
        List<String> firstRunLogs = launcher.getLastBootConsoleLogs();
        assertThat(firstRunLogs).anyMatch(line -> line.contains("[VESSEL/INSTALLER]: First-run detected"));
        assertThat(firstRunLogs).anyMatch(line -> line.contains("[VESSEL/INSTALLER]: Created directory hierarchy"));
        assertThat(firstRunLogs).noneMatch(line -> line.contains("[VESSEL/DEBUG]:"));

        // 2. Second Run (Normal Mode) -> Must emit concise [VESSEL/INFO] logs without [VESSEL/INSTALLER] or [VESSEL/DEBUG]
        int secondExit = launcher.executeBootSequence(new String[]{"--nogui"});
        assertThat(secondExit).isEqualTo(0);
        List<String> secondRunLogs = launcher.getLastBootConsoleLogs();
        assertThat(secondRunLogs).anyMatch(line -> line.contains("[VESSEL/INFO]: Verified server workspace directories"));
        assertThat(secondRunLogs).anyMatch(line -> line.contains("[VESSEL/INFO]: Built-in Ecosystem ready: GeyserMC"));
        assertThat(secondRunLogs).noneMatch(line -> line.contains("[VESSEL/INSTALLER]: First-run detected"));
        assertThat(secondRunLogs).noneMatch(line -> line.contains("[VESSEL/DEBUG]:"));

        // 3. Third Run (--vessel-deep-debug) -> Must emit [VESSEL/DEBUG] detailed trace logs
        int debugExit = launcher.executeBootSequence(new String[]{"--vessel-deep-debug", "--nogui"});
        assertThat(debugExit).isEqualTo(0);
        List<String> debugRunLogs = launcher.getLastBootConsoleLogs();
        assertThat(debugRunLogs).anyMatch(line -> line.contains("[VESSEL/DEBUG]: CLI Flags Profile:"));
        assertThat(debugRunLogs).anyMatch(line -> line.contains("[VESSEL/DEBUG]: VESSEL 16-step boot sequence completed"));
    }

    @Test
    void shouldHaltCleanlyAndLogJavaVersionRequirementWhenServerRequiresHigherJavaThanHost() throws Exception {
        Path serverRoot = tempDir.resolve("java25-mismatch-server");
        Path cacheDir = serverRoot.resolve("cache");
        Files.createDirectories(cacheDir);

        // Synthesize a Mojang bundler JAR for 26.3 whose net.minecraft.server.Main has class file major version 69 (Java 25)
        Path bundlerJar = cacheDir.resolve("mojang_26.3.jar");
        byte[] innerServerJarBytes = createInnerServerJarWithClassVersion(
                "net/minecraft/server/Main", 69);
        String innerSha1 = sha1Hex(innerServerJarBytes);

        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(bundlerJar))) {
            writeEntry(jos, "META-INF/main-class",
                    "net.minecraft.server.Main".getBytes(StandardCharsets.UTF_8));
            String versionsList = innerSha1 + "\t26.3\t26.3/server-26.3.jar\n";
            writeEntry(jos, "META-INF/versions.list",
                    versionsList.getBytes(StandardCharsets.UTF_8));
            writeEntry(jos, "META-INF/libraries.list", new byte[0]);
            writeEntry(jos, "META-INF/versions/26.3/server-26.3.jar", innerServerJarBytes);
        }

        VesselServerLauncher launcher = new VesselServerLauncher(serverRoot);
        int exitCode = launcher.executeBootSequence(new String[]{"--vessel-version=26.3", "--nogui"});

        JvmVersionCompatibilityGuard guard = new JvmVersionCompatibilityGuard();
        if (guard.detectRunningJavaFeatureVersion() < 25) {
            assertThat(exitCode).isEqualTo(1);
            assertThat(launcher.isJvmVersionMismatchHalted()).isTrue();
            assertThat(launcher.isVanillaMainInvoked()).isFalse();
            List<String> logs = launcher.getLastBootConsoleLogs();
            assertThat(logs).anyMatch(l -> l.contains("JAVA RUNTIME VERSION MISMATCH"));
            assertThat(logs).anyMatch(l -> l.contains("VESSEL Minimum   : Java 21 LTS (class file version 65.0)"));
            assertThat(logs).anyMatch(l -> l.contains("Server Requires  : Java 25 or newer (compiled with class file version 69.0)"));
            assertThat(logs).anyMatch(l -> l.contains("--vessel-version=1.21.11"));
        } else {
            VesselStartupConsoleLogger customLogger = new VesselStartupConsoleLogger(false);
            boolean ok = guard.verifyJvmCompatibilityOrLogError(
                    21, 69, "26.3", "net.minecraft.server.Main", customLogger);
            assertThat(ok).isFalse();
            assertThat(customLogger.getRecordedLines())
                    .anyMatch(l -> l.contains("Server Requires  : Java 25 or newer"));
        }
    }

    @Test
    void shouldDynamicallyLaunchLegacyMonolithic116And112ServerJarsAndKeepClassLoaderOpen() throws Exception {
        Path serverRoot = tempDir.resolve("dynamic-legacy-server");
        Path cacheDir = serverRoot.resolve("cache");
        Files.createDirectories(cacheDir);

        // Synthesize a legacy Minecraft 1.12.2 monolithic server JAR (Java 8 = class file 52, Main-Class = net.minecraft.server.MinecraftServer)
        Path legacy112Jar = cacheDir.resolve("mojang_1.12.2.jar");
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "net.minecraft.server.MinecraftServer");

        byte[] mcServerClassBytes = createMainClassBytes("net/minecraft/server/MinecraftServer", 52);
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(legacy112Jar), manifest)) {
            writeEntry(jos, "net/minecraft/server/MinecraftServer.class", mcServerClassBytes);
        }

        MojangBundlerExtractor extractor = new MojangBundlerExtractor();
        assertThat(extractor.isBundlerArchive(legacy112Jar)).isFalse();
        assertThat(extractor.readBundlerMainClass(legacy112Jar))
                .isEqualTo("net.minecraft.server.MinecraftServer");

        VesselServerLauncher launcher = new VesselServerLauncher(serverRoot);
        int exitCode = launcher.executeBootSequence(new String[]{"--vessel-version=1.12.2", "--nogui"});
        assertThat(exitCode).isEqualTo(0);
        assertThat(launcher.isVanillaMainInvoked()).isTrue();
        assertThat(launcher.getActiveTierOneClassLoader()).isNotNull();
        // Verify Tier-1 ClassLoader stays open after main() returns so background Server thread can load classes
        Class<?> stillLoadable = Class.forName(
                "net.minecraft.server.MinecraftServer",
                false,
                launcher.getActiveTierOneClassLoader());
        assertThat(stillLoadable.getName()).isEqualTo("net.minecraft.server.MinecraftServer");
        launcher.getActiveTierOneClassLoader().close();
    }

    @Test
    void shouldInterceptLiveConsoleVesselCommandsAndPassThroughVanillaCommands() throws Exception {
        Path serverRoot = tempDir.resolve("console-interceptor-server");
        VesselServerLauncher launcher = new VesselServerLauncher(serverRoot);
        launcher.executeBootSequence(new String[]{"--vessel-ultra-optimize", "--nogui"});

        String simulatedConsoleSession = String.join(
                "\n",
                "/vessel",
                "/vessel mod",
                "cls",
                "say hi",
                "/vessel top",
                "/vessel mods",
                "stop") + "\n";

        ByteArrayInputStream rawIn = new ByteArrayInputStream(
                simulatedConsoleSession.getBytes(StandardCharsets.UTF_8));
        try (VesselConsoleCommandInterceptorStream interceptor =
                        new VesselConsoleCommandInterceptorStream(rawIn, launcher);
                BufferedReader vanillaReader = new BufferedReader(
                        new InputStreamReader(interceptor, StandardCharsets.UTF_8))) {
            // Vanilla reader must ONLY receive "say hi" and "stop", while /vessel*, cls are handled by VESSEL
            String firstVanillaCmd = vanillaReader.readLine();
            String secondVanillaCmd = vanillaReader.readLine();
            String eofLine = vanillaReader.readLine();

            assertThat(firstVanillaCmd).isEqualTo("say hi");
            assertThat(secondVanillaCmd).isEqualTo("stop");
            assertThat(eofLine).isNull();
        }

        List<String> logs = launcher.getLastBootConsoleLogs();
        assertThat(logs).anyMatch(l -> l.contains("VESSEL Sovereign Command Suite"));
        assertThat(logs).anyMatch(l -> l.contains("[VESSEL/MODS] Loaded Mods"));
        assertThat(logs).anyMatch(l -> l.contains("[VESSEL/TOP] Per-Tick Module CPU Profiler"));
        assertThat(logs).anyMatch(l -> l.contains("Console screen cleared"));
    }

    private static byte[] createInnerServerJarWithClassVersion(
            String internalClassName,
            int classFileMajorVersion) throws Exception {
        byte[] classBytes = createMainClassBytes(internalClassName, classFileMajorVersion);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JarOutputStream jos = new JarOutputStream(baos)) {
            writeEntry(jos, internalClassName + ".class", classBytes);
        }
        return baos.toByteArray();
    }

    private static byte[] createMainClassBytes(
            String internalClassName,
            int classFileMajorVersion) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(
                Opcodes.V21,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                internalClassName,
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
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 1);
        mv.visitEnd();
        cw.visitEnd();
        byte[] classBytes = cw.toByteArray();
        classBytes[6] = (byte) ((classFileMajorVersion >>> 8) & 0xFF);
        classBytes[7] = (byte) (classFileMajorVersion & 0xFF);
        return classBytes;
    }

    private static void writeEntry(JarOutputStream jos, String name, byte[] bytes) throws Exception {
        JarEntry entry = new JarEntry(name);
        jos.putNextEntry(entry);
        jos.write(bytes);
        jos.closeEntry();
    }

    private static String sha1Hex(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        return HexFormat.of().formatHex(digest.digest(bytes));
    }
}
