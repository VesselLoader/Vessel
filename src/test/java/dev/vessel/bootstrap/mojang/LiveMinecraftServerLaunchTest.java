package dev.vessel.bootstrap.mojang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vessel.bootstrap.target.VesselServerLauncher;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import net.fabricmc.loader.api.FabricLoader;
import org.bukkit.Bukkit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * End-to-end verification suite for Sub-Stage 7.5: Single-pass first-run server boot with
 * automatic Mojang {@code latest.release} resolution, live multi-era bytecode transformation,
 * Fabric/NeoForge/Bukkit entrypoint activation from physical {@code .jar} files in {@code mods/}
 * and {@code plugins/}, and Mojang bundler extraction into {@code net.minecraft.server.Main}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
class LiveMinecraftServerLaunchTest {

    @Test
    void testSinglePassFirstRunActivatesAllModsAndPluginsAndLaunchesVanillaMain(@TempDir Path tempDir)
            throws Exception {
        // 1. Prepare latest.release manifest (26.1.2) and Mojang Bundler JAR in cache/
        Path cacheDir = tempDir.resolve("cache");
        Files.createDirectories(cacheDir);
        String manifestJson = """
                {
                  "latest": {
                    "release": "26.1.2",
                    "snapshot": "26w18a"
                  },
                  "versions": [
                    {
                      "id": "26.1.2",
                      "type": "release",
                      "url": "https://piston-meta.mojang.com/v1/packages/26.1.2/26.1.2.json"
                    }
                  ]
                }
                """.stripIndent();
        Files.writeString(
                cacheDir.resolve(MojangVersionManifestResolver.CACHED_MANIFEST_FILE),
                manifestJson,
                StandardCharsets.UTF_8);

        byte[] innerServerJarBytes = createInnerMinecraftServerJar();
        String innerSha1 = sha1Hex(innerServerJarBytes);
        byte[] bundlerJarBytes = createMojangBundlerJar("26.1.2", innerServerJarBytes, innerSha1);
        Files.write(cacheDir.resolve("mojang_26.1.2.jar"), bundlerJarBytes);

        // 2. Create physical mod & plugin JARs in mods/ and plugins/
        Path modsDir = tempDir.resolve("mods");
        Path pluginsDir = tempDir.resolve("plugins");
        Files.createDirectories(modsDir);
        Files.createDirectories(pluginsDir);

        createFabricModJar(modsDir.resolve("lithium-legacy.jar"));
        createNeoForgeModJar(modsDir.resolve("create-neoforge.jar"));
        createBukkitPluginJar(pluginsDir.resolve("EssentialsX-Legacy.jar"));

        // 3. Execute VesselServerLauncher a SINGLE time on a brand-new first-run directory
        VesselServerLauncher launcher = new VesselServerLauncher(tempDir);
        int exitCode = launcher.executeBootSequence(new String[]{
            "--vessel-deep-debug",
            "--vessel-ultra-optimize",
            "--nogui"
        });

        assertEquals(0, exitCode);
        assertEquals(16, launcher.getCompletedBootSteps());
        assertEquals("26.1.2", launcher.getResolvedActiveMcVersion());
        assertEquals(3, launcher.getDiscoveredModules().size());
        assertEquals(3, launcher.getActivatedEntrypointsCount());
        assertTrue(launcher.isVanillaMainInvoked());

        // 4. Verify all mods, plugins, and Vanilla Main executed in this single pass
        assertEquals("INITIALIZED", System.getProperty("vessel.live.test.fabric.mod"));
        assertEquals("SERVER_READY", System.getProperty("vessel.live.test.fabric.server"));
        assertEquals("CONSTRUCTED_WITH_BUS", System.getProperty("vessel.live.test.neoforge.mod"));
        assertEquals("ENABLED", System.getProperty("vessel.live.test.bukkit.plugin"));
        assertEquals("LAUNCHED_26.1.2", System.getProperty("vessel.live.test.vanilla.main"));

        assertTrue(FabricLoader.getInstance().isModLoaded("lithium"));
        assertTrue(FabricLoader.getInstance().isModLoaded("create"));
        assertTrue(Bukkit.getPluginManager().isPluginEnabled("essentialSX"));
        assertTrue(Files.exists(tempDir.resolve("eula.txt")));
        assertTrue(Files.exists(tempDir.resolve("server.properties")));
    }

    private static void createFabricModJar(Path targetJar) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(
                Opcodes.V21,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                "com/example/lithium/LithiumLegacyMod",
                null,
                "java/lang/Object",
                new String[]{
                    "net/fabricmc/api/ModInitializer",
                    "net/fabricmc/api/DedicatedServerModInitializer"
                });

        MethodVisitor initMv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        initMv.visitCode();
        initMv.visitVarInsn(Opcodes.ALOAD, 0);
        initMv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        initMv.visitInsn(Opcodes.RETURN);
        initMv.visitMaxs(1, 1);
        initMv.visitEnd();

        MethodVisitor mainMv = cw.visitMethod(Opcodes.ACC_PUBLIC, "onInitialize", "()V", null, null);
        mainMv.visitCode();
        mainMv.visitLdcInsn("vessel.live.test.fabric.mod");
        mainMv.visitLdcInsn("INITIALIZED");
        mainMv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/lang/System",
                "setProperty",
                "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                false);
        mainMv.visitInsn(Opcodes.POP);
        mainMv.visitInsn(Opcodes.RETURN);
        mainMv.visitMaxs(2, 1);
        mainMv.visitEnd();

        MethodVisitor srvMv = cw.visitMethod(Opcodes.ACC_PUBLIC, "onInitializeServer", "()V", null, null);
        srvMv.visitCode();
        srvMv.visitLdcInsn("vessel.live.test.fabric.server");
        srvMv.visitLdcInsn("SERVER_READY");
        srvMv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/lang/System",
                "setProperty",
                "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                false);
        srvMv.visitInsn(Opcodes.POP);
        srvMv.visitInsn(Opcodes.RETURN);
        srvMv.visitMaxs(2, 1);
        srvMv.visitEnd();
        cw.visitEnd();

        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(targetJar))) {
            writeTextEntry(jos, "fabric.mod.json", "{\"id\":\"lithium\",\"version\":\"0.14.3\"}");
            writeBinaryEntry(
                    jos,
                    "com/example/lithium/LithiumLegacyMod.class",
                    cw.toByteArray());
        }
    }

    private static void createNeoForgeModJar(Path targetJar) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(
                Opcodes.V21,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                "com/example/create/CreateNeoForgeMod",
                null,
                "java/lang/Object",
                null);

        AnnotationVisitor av = cw.visitAnnotation("Lnet/neoforged/fml/common/Mod;", true);
        av.visit("value", "create");
        av.visitEnd();

        MethodVisitor initMv = cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                "<init>",
                "(Lnet/neoforged/bus/api/IEventBus;)V",
                null,
                null);
        initMv.visitCode();
        initMv.visitVarInsn(Opcodes.ALOAD, 0);
        initMv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        initMv.visitLdcInsn("vessel.live.test.neoforge.mod");
        initMv.visitLdcInsn("CONSTRUCTED_WITH_BUS");
        initMv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/lang/System",
                "setProperty",
                "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                false);
        initMv.visitInsn(Opcodes.POP);
        initMv.visitInsn(Opcodes.RETURN);
        initMv.visitMaxs(2, 2);
        initMv.visitEnd();
        cw.visitEnd();

        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(targetJar))) {
            writeTextEntry(
                    jos,
                    "META-INF/neoforge.mods.toml",
                    "[[mods]]\nmodId=\"create\"\nversion=\"6.0.0\"\n");
            writeBinaryEntry(
                    jos,
                    "com/example/create/CreateNeoForgeMod.class",
                    cw.toByteArray());
        }
    }

    private static void createBukkitPluginJar(Path targetJar) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(
                Opcodes.V21,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                "com/example/essentials/EssentialsLegacyPlugin",
                null,
                "org/bukkit/plugin/java/JavaPlugin",
                new String[]{"org/bukkit/event/Listener"});

        MethodVisitor initMv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        initMv.visitCode();
        initMv.visitVarInsn(Opcodes.ALOAD, 0);
        initMv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "org/bukkit/plugin/java/JavaPlugin",
                "<init>",
                "()V",
                false);
        initMv.visitInsn(Opcodes.RETURN);
        initMv.visitMaxs(1, 1);
        initMv.visitEnd();

        MethodVisitor enableMv = cw.visitMethod(Opcodes.ACC_PUBLIC, "onEnable", "()V", null, null);
        enableMv.visitCode();
        enableMv.visitLdcInsn("vessel.live.test.bukkit.plugin");
        enableMv.visitLdcInsn("ENABLED");
        enableMv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/lang/System",
                "setProperty",
                "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                false);
        enableMv.visitInsn(Opcodes.POP);
        enableMv.visitInsn(Opcodes.RETURN);
        enableMv.visitMaxs(2, 1);
        enableMv.visitEnd();
        cw.visitEnd();

        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(targetJar))) {
            writeTextEntry(
                    jos,
                    "plugin.yml",
                    "name: EssentialsX\nversion: 2.20.1\nmain: com.example.essentials.EssentialsLegacyPlugin\n");
            writeBinaryEntry(
                    jos,
                    "com/example/essentials/EssentialsLegacyPlugin.class",
                    cw.toByteArray());
        }
    }

    private static byte[] createInnerMinecraftServerJar() throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
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
        mv.visitLdcInsn("vessel.live.test.vanilla.main");
        mv.visitLdcInsn("LAUNCHED_26.1.2");
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
            writeBinaryEntry(jos, "net/minecraft/server/Main.class", cw.toByteArray());
        }
        return baos.toByteArray();
    }

    private static byte[] createMojangBundlerJar(
            String version,
            byte[] innerServerJarBytes,
            String innerSha1) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JarOutputStream jos = new JarOutputStream(baos)) {
            writeTextEntry(jos, "META-INF/main-class", "net.minecraft.server.Main\n");
            writeTextEntry(
                    jos,
                    "META-INF/versions.list",
                    innerSha1 + "\t" + version + "\t" + version + "/server-" + version + ".jar\n");
            writeBinaryEntry(
                    jos,
                    "META-INF/versions/" + version + "/server-" + version + ".jar",
                    innerServerJarBytes);
        }
        return baos.toByteArray();
    }

    private static void writeTextEntry(JarOutputStream jos, String name, String content)
            throws Exception {
        writeBinaryEntry(jos, name, content.getBytes(StandardCharsets.UTF_8));
    }

    private static void writeBinaryEntry(JarOutputStream jos, String name, byte[] bytes)
            throws Exception {
        JarEntry entry = new JarEntry(name);
        jos.putNextEntry(entry);
        jos.write(bytes);
        jos.closeEntry();
    }

    private static String sha1Hex(byte[] bytes) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        return HexFormat.of().formatHex(md.digest(bytes));
    }
}
