package dev.vessel.bootstrap.ecosystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vessel.bootstrap.mojang.RealDirectoryModAndPluginScanner;
import dev.vessel.bootstrap.mojang.SinglePassModAndPluginLiveActivator;
import dev.vessel.bootstrap.mojang.VesselCustomFlagsProfile;
import dev.vessel.kernel.compat.era.ForwardVersionModRejectedException;
import dev.vessel.kernel.compat.era.ForwardVersionModRejectionGuard;
import dev.vessel.kernel.compat.era.SemanticMinecraftVersionComparator;
import dev.vessel.kernel.identity.VesselModuleIdentifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Unit tests for Sub-Stage 8.1: Strict forward-version mod/plugin rejection guard
 * ({@link ForwardVersionModRejectionGuard}) and built-in latest Geyser &amp; Spark integration with
 * full open-source credits ({@link BuiltInGeyserAndSparkProvisioner} and
 * {@link ThirdPartyCreditsAndAttributionRegistry}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
class GeyserSparkAndForwardVersionGuardTest {

    @Test
    void testForwardVersionModRejectionGuardRefusesModNewerThanServerVersion() {
        SemanticMinecraftVersionComparator comparator = new SemanticMinecraftVersionComparator();
        assertTrue(comparator.isServerVersionLowerThanMod(">=1.21.4", "1.20.1"));
        assertFalse(comparator.isServerVersionLowerThanMod(">=1.19.4", "1.20.1"));
        assertFalse(comparator.isServerVersionLowerThanMod("1.20.1", "1.20.1"));

        ForwardVersionModRejectionGuard guard = new ForwardVersionModRejectionGuard();
        // Lower-version mod on higher server -> allowed
        guard.enforceServerVersionNotLowerThanMod("sodium", ">=1.19.4", "1.21.4");

        // Higher-version mod on lower server -> strictly rejected with remediation instructions
        ForwardVersionModRejectedException ex = assertThrows(
                ForwardVersionModRejectedException.class,
                () -> guard.enforceServerVersionNotLowerThanMod("create", ">=1.21.4", "1.20.1"));
        assertEquals("create", ex.getModuleId());
        assertEquals("1.21.4", ex.getRequiredMinecraftVersion());
        assertEquals("1.20.1", ex.getActiveServerMinecraftVersion());
        assertTrue(ex.getMessage().contains("install a lower build of 'create'"));
        assertTrue(ex.getMessage().contains("--vessel-version=1.21.4"));
    }

    @Test
    void testLiveActivatorLoadsLowerVersionModAndRejectsHigherVersionMod(@TempDir Path tempDir)
            throws Exception {
        Path modsDir = tempDir.resolve("mods");
        Files.createDirectories(modsDir);

        createFabricModJarWithMcRequirement(
                modsDir.resolve("legacy-mod-1.19.4.jar"),
                "legacymod",
                ">=1.19.4",
                "com/example/legacy/LegacyCompatibleMod",
                "vessel.test.legacy.loaded");
        createFabricModJarWithMcRequirement(
                modsDir.resolve("future-mod-1.21.4.jar"),
                "futuremod",
                ">=1.21.4",
                "com/example/future/FutureRejectedMod",
                "vessel.test.future.loaded");

        RealDirectoryModAndPluginScanner scanner = new RealDirectoryModAndPluginScanner();
        Map<Path, VesselModuleIdentifier> discovered = scanner.scanAllModAndPluginJars(tempDir);
        assertEquals(2, discovered.size());

        System.clearProperty("vessel.test.legacy.loaded");
        System.clearProperty("vessel.test.future.loaded");

        SinglePassModAndPluginLiveActivator activator = new SinglePassModAndPluginLiveActivator();
        int activated = activator.activateAllDiscoveredModsAndPlugins(
                tempDir,
                "1.20.1",
                discovered,
                new VesselCustomFlagsProfile(),
                GeyserSparkAndForwardVersionGuardTest.class.getClassLoader());

        assertEquals(1, activated);
        assertEquals("YES", System.getProperty("vessel.test.legacy.loaded"));
        assertEquals(null, System.getProperty("vessel.test.future.loaded"));
        assertTrue(activator.getRejectedForwardVersionModules().containsKey("futuremod"));
        assertTrue(activator.getRejectedForwardVersionModules().get("futuremod")
                .contains("requires Minecraft 1.21.4"));
    }

    @Test
    void testBuiltInGeyserAndSparkProvisionerAndCreditsGeneration(@TempDir Path tempDir)
            throws Exception {
        BuiltInGeyserAndSparkProvisioner provisioner = new BuiltInGeyserAndSparkProvisioner();
        assertEquals(
                "2.6.2-b800",
                provisioner.parseGeyserVersionFromJson("{\"version\":\"2.6.2\",\"build\":800}"));
        assertEquals(
                "1.10.125",
                provisioner.parseSparkVersionFromJson("[{\"version_number\":\"1.10.125\"}]"));

        Path stateFile = provisioner.provisionAndSyncLatestEcosystem(
                tempDir,
                "1.21.4",
                "offline://geyser",
                "offline://spark");
        assertTrue(Files.exists(stateFile));
        assertTrue(Files.exists(
                tempDir.resolve("cache/ecosystem/geyser-vessel-integrated.jar")));
        assertTrue(Files.exists(
                tempDir.resolve("cache/ecosystem/spark-vessel-integrated.jar")));

        Path creditsFile = tempDir.resolve(ThirdPartyCreditsAndAttributionRegistry.CREDITS_FILE_NAME);
        assertTrue(Files.exists(creditsFile));
        String creditsContent = Files.readString(creditsFile, StandardCharsets.UTF_8);
        assertTrue(creditsContent.contains("GeyserMC"));
        assertTrue(creditsContent.contains("Lucko (lucko)"));
        assertTrue(creditsContent.contains("https://geysermc.org"));
        assertTrue(creditsContent.contains("https://spark.lucko.me"));
    }

    private static void createFabricModJarWithMcRequirement(
            Path targetJar,
            String modId,
            String mcConstraint,
            String internalClassName,
            String propertyKey) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(
                Opcodes.V21,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                internalClassName,
                null,
                "java/lang/Object",
                new String[]{"net/fabricmc/api/ModInitializer"});

        MethodVisitor initMv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        initMv.visitCode();
        initMv.visitVarInsn(Opcodes.ALOAD, 0);
        initMv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        initMv.visitInsn(Opcodes.RETURN);
        initMv.visitMaxs(1, 1);
        initMv.visitEnd();

        MethodVisitor mainMv = cw.visitMethod(Opcodes.ACC_PUBLIC, "onInitialize", "()V", null, null);
        mainMv.visitCode();
        mainMv.visitLdcInsn(propertyKey);
        mainMv.visitLdcInsn("YES");
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
        cw.visitEnd();

        String fabricJson = """
                {
                  "id": "%s",
                  "version": "1.0.0",
                  "depends": {
                    "minecraft": "%s"
                  }
                }
                """.stripIndent().formatted(modId, mcConstraint);

        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(targetJar))) {
            JarEntry jsonEntry = new JarEntry("fabric.mod.json");
            jos.putNextEntry(jsonEntry);
            jos.write(fabricJson.getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();

            JarEntry classEntry = new JarEntry(internalClassName + ".class");
            jos.putNextEntry(classEntry);
            jos.write(cw.toByteArray());
            jos.closeEntry();
        }
    }
}
