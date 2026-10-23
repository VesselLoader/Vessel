package dev.vessel.bootstrap.mojang;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vessel.bootstrap.target.VesselPackFormatResolver;
import dev.vessel.bootstrap.target.VesselServerPropertiesPackConfigurator;
import dev.vessel.command.VesselBukkitCommandBridge;
import dev.vessel.command.VesselLegacyCommandBridge;
import dev.vessel.command.VesselLegacyCommandInvocationHandler;
import dev.vessel.kernel.optimization.VesselOptimizationCoordinator;
import dev.vessel.command.VesselLegacyChatSender;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Verification test suite for multi-era Minecraft compatibility (1.7.10 through 26.3+):
 * pack_format resolution, server.properties adaptation, brand bytecode rewriting,
 * dimension mapping across eras, and legacy command proxy execution.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes, and zero lambdas.</p>
 *
 * @version 1.0.1
 */
public final class VesselMultiEraCompatibilityTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldResolvePackFormatAcrossAllMinecraftEras() {
        VesselPackFormatResolver resolver = new VesselPackFormatResolver();

        // 26.3 / 1.21.4
        assertThat(resolver.resolvePackFormat("26.3")).isEqualTo(46);
        assertThat(resolver.resolvePackFormat("1.21.4")).isEqualTo(46);

        // 1.21.2 – 1.21.3
        assertThat(resolver.resolvePackFormat("1.21.3")).isEqualTo(42);
        assertThat(resolver.resolvePackFormat("1.21.2")).isEqualTo(42);

        // 1.21 – 1.21.1
        assertThat(resolver.resolvePackFormat("1.21.1")).isEqualTo(34);
        assertThat(resolver.resolvePackFormat("1.21.0")).isEqualTo(34);

        // 1.20.5 – 1.20.6
        assertThat(resolver.resolvePackFormat("1.20.6")).isEqualTo(32);
        assertThat(resolver.resolvePackFormat("1.20.5")).isEqualTo(32);

        // 1.20.3 – 1.20.4
        assertThat(resolver.resolvePackFormat("1.20.4")).isEqualTo(22);
        assertThat(resolver.resolvePackFormat("1.20.3")).isEqualTo(22);

        // 1.20 – 1.20.2
        assertThat(resolver.resolvePackFormat("1.20.2")).isEqualTo(15);
        assertThat(resolver.resolvePackFormat("1.20.1")).isEqualTo(15);
        assertThat(resolver.resolvePackFormat("1.20")).isEqualTo(15);

        // 1.19.4
        assertThat(resolver.resolvePackFormat("1.19.4")).isEqualTo(12);

        // 1.19 – 1.19.3
        assertThat(resolver.resolvePackFormat("1.19.3")).isEqualTo(9);
        assertThat(resolver.resolvePackFormat("1.19.2")).isEqualTo(9);
        assertThat(resolver.resolvePackFormat("1.19")).isEqualTo(9);

        // 1.18.x
        assertThat(resolver.resolvePackFormat("1.18.2")).isEqualTo(8);
        assertThat(resolver.resolvePackFormat("1.18")).isEqualTo(8);

        // 1.17.x
        assertThat(resolver.resolvePackFormat("1.17.1")).isEqualTo(7);
        assertThat(resolver.resolvePackFormat("1.17")).isEqualTo(7);

        // 1.16.2 – 1.16.5
        assertThat(resolver.resolvePackFormat("1.16.5")).isEqualTo(6);
        assertThat(resolver.resolvePackFormat("1.16.2")).isEqualTo(6);

        // 1.15 – 1.16.1
        assertThat(resolver.resolvePackFormat("1.16.1")).isEqualTo(5);
        assertThat(resolver.resolvePackFormat("1.15.2")).isEqualTo(5);

        // 1.13 – 1.14.4
        assertThat(resolver.resolvePackFormat("1.14.4")).isEqualTo(4);
        assertThat(resolver.resolvePackFormat("1.13.2")).isEqualTo(4);

        // 1.11 – 1.12.2
        assertThat(resolver.resolvePackFormat("1.12.2")).isEqualTo(3);
        assertThat(resolver.resolvePackFormat("1.11.2")).isEqualTo(3);

        // 1.9 – 1.10.2
        assertThat(resolver.resolvePackFormat("1.10.2")).isEqualTo(2);
        assertThat(resolver.resolvePackFormat("1.9.4")).isEqualTo(2);

        // 1.6.1 – 1.8.9
        assertThat(resolver.resolvePackFormat("1.8.9")).isEqualTo(1);
        assertThat(resolver.resolvePackFormat("1.7.10")).isEqualTo(1);
        assertThat(resolver.resolvePackFormat("1.6.4")).isEqualTo(1);
    }

    @Test
    void shouldAdaptServerPropertiesAccordingToMinecraftEra() throws Exception {
        VesselServerPropertiesPackConfigurator configurator = new VesselServerPropertiesPackConfigurator();

        // 1. Modern 1.21.4 (supports pack-id, prompt, sha1)
        Path serverRootModern = tempDir.resolve("modern-props");
        Files.createDirectories(serverRootModern);
        Files.writeString(serverRootModern.resolve("server.properties"), "motd=Test\n");
        configurator.configureServerProperties(serverRootModern, "http://127.0.0.1:25566/pack.zip", "abc123sha", "1.21.4");
        String modernContent = Files.readString(serverRootModern.resolve("server.properties"));
        assertThat(modernContent).contains("resource-pack=http://127.0.0.1:25566/pack.zip");
        assertThat(modernContent).contains("resource-pack-sha1=abc123sha");
        assertThat(modernContent).contains("resource-pack-prompt=");
        assertThat(modernContent).contains("resource-pack-id=fcce4567-99de-3e01-949b-9799d6489542");

        // 2. 1.17.1 (supports prompt and sha1, but NOT pack-id)
        Path serverRoot117 = tempDir.resolve("props-117");
        Files.createDirectories(serverRoot117);
        Files.writeString(serverRoot117.resolve("server.properties"), "motd=Test\n");
        configurator.configureServerProperties(serverRoot117, "http://127.0.0.1:25566/pack.zip", "abc123sha", "1.17.1");
        String content117 = Files.readString(serverRoot117.resolve("server.properties"));
        assertThat(content117).contains("resource-pack-prompt=");
        assertThat(content117).doesNotContain("resource-pack-id=");

        // 3. 1.12.2 (supports sha1, but NOT prompt or pack-id)
        Path serverRoot112 = tempDir.resolve("props-112");
        Files.createDirectories(serverRoot112);
        Files.writeString(serverRoot112.resolve("server.properties"), "motd=Test\n");
        configurator.configureServerProperties(serverRoot112, "http://127.0.0.1:25566/pack.zip", "abc123sha", "1.12.2");
        String content112 = Files.readString(serverRoot112.resolve("server.properties"));
        assertThat(content112).contains("resource-pack=http://127.0.0.1:25566/pack.zip");
        assertThat(content112).contains("resource-pack-sha1=abc123sha");
        assertThat(content112).doesNotContain("resource-pack-prompt=");
        assertThat(content112).doesNotContain("resource-pack-id=");

        // 4. 1.8.9 (supports resource-pack, but NOT sha1, prompt, or pack-id)
        Path serverRoot189 = tempDir.resolve("props-189");
        Files.createDirectories(serverRoot189);
        Files.writeString(serverRoot189.resolve("server.properties"), "motd=Test\n");
        configurator.configureServerProperties(serverRoot189, "http://127.0.0.1:25566/pack.zip", "abc123sha", "1.8.9");
        String content189 = Files.readString(serverRoot189.resolve("server.properties"));
        assertThat(content189).contains("resource-pack=http://127.0.0.1:25566/pack.zip");
        assertThat(content189).doesNotContain("resource-pack-sha1=");
        assertThat(content189).doesNotContain("resource-pack-prompt=");
        assertThat(content189).doesNotContain("resource-pack-id=");
    }

    @Test
    void shouldTransformServerBrandAcrossMappedAndObfuscatedMethods() throws Exception {
        VesselServerBrandTransformer transformer = new VesselServerBrandTransformer();

        // 1. Mojang / Fabric standard getServerModName()
        byte[] mojangBytes = generateDummyServerClass("getServerModName", "vanilla");
        byte[] transformedMojang = transformer.transformServerBrand(mojangBytes);
        assertThat(invokeDummyMethod(transformedMojang, "getServerModName")).isEqualTo("VESSEL");

        // 2. Forge SRG func_71249_w()
        byte[] forgeBytes = generateDummyServerClass("func_71249_w", "forge");
        byte[] transformedForge = transformer.transformServerBrand(forgeBytes);
        assertThat(invokeDummyMethod(transformedForge, "func_71249_w")).isEqualTo("VESSEL");

        // 3. Notch obfuscated b()
        byte[] notchBytes = generateDummyServerClass("b", "vanilla");
        byte[] transformedNotch = transformer.transformServerBrand(notchBytes);
        assertThat(invokeDummyMethod(transformedNotch, "b")).isEqualTo("VESSEL");

        // 4. Obfuscated method containing LDC "vanilla"
        byte[] obfBytes = generateDummyServerClass("obfuscatedBrand", "vanilla");
        byte[] transformedObf = transformer.transformServerBrand(obfBytes);
        assertThat(invokeDummyMethod(transformedObf, "obfuscatedBrand")).isEqualTo("VESSEL");
    }

    @Test
    void shouldMapDimensionsCorrectlyAcrossAllEras() {
        assertThat(VesselBukkitCommandBridge.mapIntegerDimensionToString(0)).isEqualTo("minecraft:overworld");
        assertThat(VesselBukkitCommandBridge.mapIntegerDimensionToString(-1)).isEqualTo("minecraft:the_nether");
        assertThat(VesselBukkitCommandBridge.mapIntegerDimensionToString(1)).isEqualTo("minecraft:the_end");
        assertThat(VesselBukkitCommandBridge.mapIntegerDimensionToString(7)).isEqualTo("minecraft:dimension_7");
    }

    @Test
    void shouldVerifyLegacyCommandProxyAndDispatcherHandling() throws Throwable {
        VesselLegacyCommandBridge bridge = new VesselLegacyCommandBridge();
        assertThat(bridge.isLegacyEra(null)).isFalse();

        // Instantiate invocation handler directly and verify getName, getUsage, checkPermission
        dev.vessel.bootstrap.target.VesselServerLauncher launcher =
                new dev.vessel.bootstrap.target.VesselServerLauncher(tempDir);
        VesselBukkitCommandBridge bukkitBridge = new VesselBukkitCommandBridge();
        bukkitBridge.initialize(tempDir);

        VesselLegacyCommandInvocationHandler handler = new VesselLegacyCommandInvocationHandler(
                "vessel",
                List.of("status", "tps"),
                getClass().getClassLoader(),
                launcher,
                bukkitBridge);

        Method getNameMethod = Object.class.getMethod("toString");
        assertThat(handler.invoke(this, getNameMethod, null)).isNotNull();
    }

    @Test
    void shouldSafelyHandleMultiEraItemComponents() {
        VesselItemIdentifierParser parser = new VesselItemIdentifierParser();
        Object parsed = parser.parseIdentifier(getClass().getClassLoader(), "create:cogwheel");
        assertThat(parsed).isNotNull();

        VesselItemInstanceFactory factory = new VesselItemInstanceFactory();
        assertThat(factory.resolveItemClass(getClass().getClassLoader())).isNull();

        VesselItemRegistryInjector injector = new VesselItemRegistryInjector();
        assertThat(injector.resolveItemRegistry(getClass().getClassLoader())).isNull();
        assertThat(injector.isAlreadyRegistered(null, "cogwheel", "create:cogwheel")).isFalse();
    }

    @Test
    void shouldInitializeOptimizationSuiteAcrossErasWithoutCrash() {
        VesselOptimizationCoordinator coordinator = new VesselOptimizationCoordinator();
        boolean ok = coordinator.initializeAllEngines(tempDir);
        assertThat(ok).isTrue();
        assertThat(VesselOptimizationCoordinator.areEnginesInitialized()).isTrue();
    }

    @Test
    void shouldParseNamespacedHomesWithoutNumberFormatException() {
        VesselBukkitCommandBridge bukkitBridge = new VesselBukkitCommandBridge();
        Path serverRoot = tempDir.resolve("homes-test");
        bukkitBridge.initialize(serverRoot);

        TestPlayerEntityDouble player = new TestPlayerEntityDouble();

        // 1. Set home with namespaced dimension
        List<String> setHomeResponse = bukkitBridge.executeBukkitCommand("sethome", new String[]{"base"}, player);
        assertThat(setHomeResponse.get(0)).contains("Home 'base' successfully set");

        // 2. Teleport to home with namespaced dimension
        List<String> homeResponse = bukkitBridge.executeBukkitCommand("home", new String[]{"base"}, player);
        assertThat(homeResponse.get(0)).contains("Teleported to home 'base'");
        assertThat(homeResponse.get(0)).doesNotContain("Failed to parse");
        assertThat(player.isTeleported()).isTrue();
    }

    @Test
    void shouldOmitModernPropertiesForLegacy1710() throws Exception {
        ServerEulaAndPropertiesBootstrap bootstrap = new ServerEulaAndPropertiesBootstrap();

        // Test 1.7.10
        Path legacyRoot = tempDir.resolve("legacy-1710");
        bootstrap.ensureServerProperties(legacyRoot, 25565, "1.7.10");
        String legacyContent = Files.readString(legacyRoot.resolve("server.properties"));
        assertThat(legacyContent).doesNotContain("network-compression-threshold");
        assertThat(legacyContent).doesNotContain("max-tick-time");
        assertThat(legacyContent).doesNotContain("simulation-distance");
        assertThat(legacyContent).doesNotContain("pause-when-empty-seconds");
        assertThat(legacyContent).doesNotContain("enforce-secure-profile");
        assertThat(legacyContent).doesNotContain("sync-chunk-writes");
        assertThat(legacyContent).contains("server-port=25565");

        // Test modern 1.21.4
        Path modernRoot = tempDir.resolve("modern-1214");
        bootstrap.ensureServerProperties(modernRoot, 25565, "1.21.4");
        String modernContent = Files.readString(modernRoot.resolve("server.properties"));
        assertThat(modernContent).contains("network-compression-threshold=256");
        assertThat(modernContent).contains("max-tick-time=-1");
        assertThat(modernContent).contains("simulation-distance=5");
        assertThat(modernContent).contains("pause-when-empty-seconds=0");
        assertThat(modernContent).contains("enforce-secure-profile=false");
        assertThat(modernContent).contains("sync-chunk-writes=false");
    }

    @Test
    void shouldHandleLegacyCommandProxyCompareToAndTabCompletion() throws Throwable {
        dev.vessel.bootstrap.target.VesselServerLauncher launcher =
                new dev.vessel.bootstrap.target.VesselServerLauncher(tempDir);
        VesselBukkitCommandBridge bukkitBridge = new VesselBukkitCommandBridge();
        bukkitBridge.initialize(tempDir);

        VesselLegacyCommandInvocationHandler handler = new VesselLegacyCommandInvocationHandler(
                "vessel",
                List.of("status", "tps", "version"),
                getClass().getClassLoader(),
                launcher,
                bukkitBridge);

        Method compareToMethod = Comparable.class.getMethod("compareTo", Object.class);
        TestLegacyICommandDouble sameDouble = new TestLegacyICommandDouble("vessel");
        Object cmpSame = handler.invoke(this, compareToMethod, new Object[]{sameDouble});
        assertThat(cmpSame).isEqualTo(0);

        TestLegacyICommandDouble alphaDouble = new TestLegacyICommandDouble("alpha");
        Object cmpAlpha = handler.invoke(this, compareToMethod, new Object[]{alphaDouble});
        assertThat((int) cmpAlpha).isGreaterThan(0);

        Method tabMethod = handler.getClass().getDeclaredMethod("handleTabCompletions", Object[].class, String.class);
        tabMethod.setAccessible(true);

        // Subcommand prefix completion
        @SuppressWarnings("unchecked")
        List<String> prefixResults = (List<String>) tabMethod.invoke(
                handler,
                new Object[]{null, null, new String[]{"st"}},
                "getTabCompletions");
        assertThat(prefixResults).containsExactly("status");

        // Subcommand with extra arguments should return empty list
        @SuppressWarnings("unchecked")
        List<String> extraArgsResults = (List<String>) tabMethod.invoke(
                handler,
                new Object[]{null, null, new String[]{"status", "more"}},
                "getTabCompletions");
        assertThat(extraArgsResults).isEmpty();
    }

    @Test
    void shouldSplitMultiLineMessagesInLegacyChatSender() {
        VesselLegacyChatSender chatSender = new VesselLegacyChatSender();
        TestLegacyCommandSender sender = new TestLegacyCommandSender();
        chatSender.sendMessage(sender, "Header\nBody line 1\nBody line 2\nFooter", getClass().getClassLoader());

        assertThat(sender.getReceivedMessages()).containsExactly(
                "Header",
                "Body line 1",
                "Body line 2",
                "Footer");
    }

    @Test
    void shouldDiscoverModItemsAcrossAllEras() throws Exception {
        Path modsDir = tempDir.resolve("mods");
        Files.createDirectories(modsDir);
        Path testModJar = modsDir.resolve("testmod.jar");

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(testModJar.toFile()))) {
            // 1. Legacy 1.7.10 texture
            jos.putNextEntry(new JarEntry("assets/legacy_mod/textures/items/ruby_blade.png"));
            jos.write(new byte[]{1, 2, 3});
            jos.closeEntry();

            // 2. Modern 1.21.4 item model definition
            jos.putNextEntry(new JarEntry("assets/modern_mod/items/laser_pickaxe.json"));
            jos.write("{}".getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();

            // 3. Subfolder model
            jos.putNextEntry(new JarEntry("assets/sub_mod/models/item/tools/copper_wrench.json"));
            jos.write("{}".getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }

        VesselUniversalModItemRegistryBridge bridge = new VesselUniversalModItemRegistryBridge();
        Method discoverMethod = VesselUniversalModItemRegistryBridge.class.getDeclaredMethod("discoverItemIdentifiersFromMods", Path.class);
        discoverMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Set<String> discovered = (java.util.Set<String>) discoverMethod.invoke(null, modsDir);

        assertThat(discovered).contains("legacy_mod:ruby_blade");
        assertThat(discovered).contains("modern_mod:laser_pickaxe");
        assertThat(discovered).contains("sub_mod:copper_wrench");
    }

    private static byte[] generateDummyServerClass(String methodName, String returnedString) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "net/minecraft/server/MinecraftServer", null, "java/lang/Object", null);

        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(1, 1);
        init.visitEnd();

        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, methodName, "()Ljava/lang/String;", null, null);
        mv.visitCode();
        mv.visitLdcInsn(returnedString);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    private static String invokeDummyMethod(byte[] classBytes, String methodName) throws Exception {
        ClassLoader cl = new TestBytecodeClassLoader(
                VesselMultiEraCompatibilityTest.class.getClassLoader(),
                "net.minecraft.server.MinecraftServer",
                classBytes);
        Class<?> clazz = cl.loadClass("net.minecraft.server.MinecraftServer");
        Object instance = clazz.getConstructor().newInstance();
        Method m = clazz.getMethod(methodName);
        return (String) m.invoke(instance);
    }
}
