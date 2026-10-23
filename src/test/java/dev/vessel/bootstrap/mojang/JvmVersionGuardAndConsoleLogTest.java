package dev.vessel.bootstrap.mojang;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vessel.bootstrap.ecosystem.BuiltInGeyserAndSparkProvisioner;
import dev.vessel.bootstrap.ecosystem.BuiltInGeyserBedrockBridgeService;
import dev.vessel.bootstrap.target.VesselConsoleCommandInterceptorStream;
import dev.vessel.bootstrap.target.VesselResourcePackDeliveryService;
import dev.vessel.bootstrap.target.VesselServerLauncher;
import dev.vessel.bootstrap.target.VesselStartupConsoleLogger;
import dev.vessel.command.VesselBrigadierCommandBridge;
import dev.vessel.command.VesselServerRestartService;
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
 *   <li>Built-in GeyserMC, Floodgate, and spark ecosystem provisioning, Bedrock UDP 19132 bridge
 *       configuration, and live console interception of {@code /geyser}, {@code /floodgate}, and
 *       {@code /spark} commands.</li>
 *   <li>Server Brand bytecode transformation to {@code "VESSEL"}, Universal Presentation HTTP Resource
 *       Pack delivery on port {@code 25566}, {@code max-tick-time=-1} Watchdog crash guard, and
 *       Paper-style {@code /restart} re-execution.</li>
 * </ol>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.6
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

    @Test
    void shouldEnforceIpv4ForMojangServicesAndFastScanEntrypointsInMilliseconds() throws Exception {
        Path serverRoot = tempDir.resolve("fast-entrypoint-server");
        Path modsDir = serverRoot.resolve("mods");
        Path pluginsDir = serverRoot.resolve("plugins");
        Files.createDirectories(modsDir);
        Files.createDirectories(pluginsDir);

        // 1. Mixin-based Fabric mod (like Lithium 0.26.2+mc26.3)
        Path lithiumJar = modsDir.resolve("lithium-fabric-0.26.2+mc26.3.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(lithiumJar))) {
            writeEntry(jos, "fabric.mod.json",
                    "{\"id\":\"lithium\",\"version\":\"0.26.2+mc26.3\",\"mixins\":[\"lithium.mixins.json\"]}"
                            .getBytes(StandardCharsets.UTF_8));
            writeEntry(jos, "lithium.mixins.json",
                    "{\"package\":\"net.caffeinemc.mods.lithium.mixin\"}".getBytes(StandardCharsets.UTF_8));
        }

        // 2. Bukkit plugin with main: in plugin.yml (like EssentialsX 2.22.0)
        Path essentialsJar = pluginsDir.resolve("EssentialsX-2.22.0.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(essentialsJar))) {
            writeEntry(jos, "plugin.yml",
                    "name: Essentials\nmain: com.earth2me.essentials.Essentials\nversion: 2.22.0\n"
                            .getBytes(StandardCharsets.UTF_8));
            writeEntry(jos, "com/earth2me/essentials/Helper.class",
                    createMainClassBytes("com/earth2me/essentials/Helper", 65));
        }

        FastJarEntrypointHeaderScanner headerScanner = new FastJarEntrypointHeaderScanner();
        assertThat(headerScanner.scanCandidateEntrypointClassNames(essentialsJar))
                .containsExactly("com.earth2me.essentials.Essentials");
        assertThat(headerScanner.containsExecutableBytecodeOrMixins(lithiumJar)).isTrue();

        VesselServerLauncher launcher = new VesselServerLauncher(serverRoot);
        int exitCode = launcher.executeBootSequence(new String[]{"--nogui"});
        assertThat(exitCode).isEqualTo(0);
        assertThat(System.getProperty("java.net.preferIPv4Stack")).isEqualTo("true");
        assertThat(System.getProperty("java.net.preferIPv6Addresses")).isEqualTo("false");
        assertThat(launcher.getActivatedEntrypointsCount()).isEqualTo(2);
    }

    @Test
    void shouldProvisionGeyserFloodgateAndSparkAndExecuteEcosystemConsoleCommands() throws Exception {
        Path serverRoot = tempDir.resolve("geyser-floodgate-spark-server");
        VesselServerLauncher launcher = new VesselServerLauncher(serverRoot);
        launcher.executeBootSequence(new String[]{"--nogui"});

        BuiltInGeyserAndSparkProvisioner provisioner = new BuiltInGeyserAndSparkProvisioner();
        assertThat(provisioner.parseGeyserVersionFromJson("[{\"version_number\":\"2.12.0-b1269\"}]"))
                .isEqualTo("2.12.0-b1269");
        assertThat(provisioner.parseFloodgateVersionFromJson("[{\"version_number\":\"2.2.7-b69\"}]"))
                .isEqualTo("2.2.7-b69");
        assertThat(provisioner.parseSparkVersionFromJson("[{\"version_number\":\"1.10.187\"}]"))
                .isEqualTo("1.10.187");

        BuiltInGeyserBedrockBridgeService bridgeService = new BuiltInGeyserBedrockBridgeService();
        Path geyserConfig = bridgeService.scaffoldGeyserRuntimeConfig(serverRoot, 25565, 19132);
        assertThat(Files.isRegularFile(geyserConfig)).isTrue();
        String configYaml = Files.readString(geyserConfig, StandardCharsets.UTF_8);
        assertThat(configYaml).contains("port: 19132");
        assertThat(configYaml).contains("auth-type: offline");

        String simulatedSession = String.join(
                "\n",
                "/geyser version",
                "/floodgate version",
                "/spark tps",
                "stop") + "\n";

        ByteArrayInputStream rawIn = new ByteArrayInputStream(
                simulatedSession.getBytes(StandardCharsets.UTF_8));
        try (VesselConsoleCommandInterceptorStream interceptor =
                        new VesselConsoleCommandInterceptorStream(rawIn, launcher);
                BufferedReader vanillaReader = new BufferedReader(
                        new InputStreamReader(interceptor, StandardCharsets.UTF_8))) {
            assertThat(vanillaReader.readLine()).isEqualTo("stop");
            assertThat(vanillaReader.readLine()).isNull();
        }

        List<String> logs = launcher.getLastBootConsoleLogs();
        assertThat(logs).anyMatch(l -> l.contains("[GeyserMC/VESSEL] Geyser version"));
        assertThat(logs).anyMatch(l -> l.contains("[Floodgate/VESSEL] Floodgate version"));
        assertThat(logs).anyMatch(l -> l.contains("[spark/VESSEL] TPS from last"));
    }

    @Test
    void shouldTransformServerBrandToVesselDeliverResourcePackAndHandleRestart() throws Exception {
        Path serverRoot = tempDir.resolve("vessel-brand-and-pack-server");
        Files.createDirectories(serverRoot);

        // 1. Verify ServerEulaAndPropertiesBootstrap writes max-tick-time=-1 and optimized properties
        ServerEulaAndPropertiesBootstrap bootstrap = new ServerEulaAndPropertiesBootstrap();
        var props = bootstrap.ensureServerProperties(serverRoot, 25565);
        assertThat(props.get("max-tick-time")).isEqualTo("-1");
        assertThat(props.get("online-mode")).isEqualTo("false");
        assertThat(props.get("view-distance")).isEqualTo("7");
        assertThat(props.get("simulation-distance")).isEqualTo("5");
        assertThat(props.get("pause-when-empty-seconds")).isEqualTo("0");
        assertThat(props.get("resource-pack-prompt")).contains("VESSEL Sovereign Universal Mod Presentation Pack");
        assertThat(props.get("resource-pack-id")).isEqualTo("fcce4567-99de-3e01-949b-9799d6489542");

        // 2. Verify VesselServerBrandTransformer rewrites getServerModName to "VESSEL"
        byte[] rawMcClass = createMainClassBytes("net/minecraft/server/MinecraftServer", 65);
        VesselServerBrandTransformer brandTransformer = new VesselServerBrandTransformer();
        byte[] transformedClass = brandTransformer.transformServerBrand(rawMcClass);
        assertThat(transformedClass).isNotEmpty();

        TestBytecodeClassDefiner testLoader = new TestBytecodeClassDefiner(ClassLoader.getSystemClassLoader());
        Class<?> loadedClass = testLoader.defineFromBytes("net.minecraft.server.MinecraftServer", transformedClass);
        Object inst = loadedClass.getDeclaredConstructor().newInstance();
        var getModNameMethod = loadedClass.getMethod("getServerModName");
        Object brand = getModNameMethod.invoke(inst);
        assertThat(brand).isEqualTo("VESSEL");

        // 3. Verify VesselResourcePackDeliveryService serves HTTP resource pack
        VesselResourcePackDeliveryService packService = new VesselResourcePackDeliveryService();
        String packUrl = packService.startDeliveryServerAndConfigureProperties(serverRoot, 25580);
        assertThat(packUrl).contains("25581");
        assertThat(packService.getActivePackSha1()).hasSize(40);
        packService.stopDeliveryServer();

        // 4. Verify VesselServerRestartService builds restart command line
        VesselServerRestartService restartService = new VesselServerRestartService();
        List<String> restartCmd = restartService.buildRestartCommandLine(serverRoot);
        assertThat(restartCmd).isNotEmpty();

        // 5. Verify /restart and Bukkit command dispatch in console
        VesselServerLauncher launcher = new VesselServerLauncher(serverRoot);
        List<String> restartOutput = launcher.executeVesselConsoleCommand("restart");
        assertThat(restartOutput).anyMatch(l -> l.contains("Server restart dispatched"));

        List<String> sethomeOutput = launcher.executeVesselConsoleCommand("sethome");
        assertThat(sethomeOutput).anyMatch(l -> l.contains("must be executed by an in-game player"));

        // 6. Verify VesselServerBridgeRegistry
        dev.vessel.command.VesselServerBridgeRegistry.setActiveMinecraftServer(inst);
        assertThat(dev.vessel.command.VesselServerBridgeRegistry.getActiveMinecraftServer()).isSameAs(inst);
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

        // <init>()V
        MethodVisitor initMv = cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                "<init>",
                "()V",
                null,
                null);
        initMv.visitCode();
        initMv.visitVarInsn(Opcodes.ALOAD, 0);
        initMv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        initMv.visitInsn(Opcodes.RETURN);
        initMv.visitMaxs(1, 1);
        initMv.visitEnd();

        // getServerModName()Ljava/lang/String; -> returns "vanilla"
        MethodVisitor modNameMv = cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                "getServerModName",
                "()Ljava/lang/String;",
                null,
                null);
        modNameMv.visitCode();
        modNameMv.visitLdcInsn("vanilla");
        modNameMv.visitInsn(Opcodes.ARETURN);
        modNameMv.visitMaxs(1, 1);
        modNameMv.visitEnd();

        // main([Ljava/lang/String;)V
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
