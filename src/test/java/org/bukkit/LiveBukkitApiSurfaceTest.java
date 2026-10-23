package org.bukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.invoke.MethodHandles;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Unit tests for Sub-Stage 7.3: Live Bukkit, Spigot, Paper, and Folia runtime API surface
 * ({@code org.bukkit.*}) verifying dynamic {@link JavaPlugin} instantiation, lifecycle hooks
 * ({@code onLoad}, {@code onEnable}, {@code onDisable}), configuration access, and reflective
 * {@link Listener} event dispatch via {@link Bukkit#getPluginManager()}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
class LiveBukkitApiSurfaceTest {

    @Test
    void testBukkitSingletonJavaPluginLifecycleAndConfigSurface() {
        Server customServer = new Server("VESSEL-Paper", "1.21.4-R0.1", "1.21.4-R0.1");
        Bukkit.setServer(customServer);
        assertEquals("VESSEL-Paper", Bukkit.getName());
        assertEquals("1.21.4-R0.1", Bukkit.getVersion());
        assertEquals("1.21.4-R0.1", Bukkit.getBukkitVersion());
        assertNotNull(Bukkit.getLogger());

        JavaPlugin samplePlugin = new JavaPlugin();
        PluginDescriptionFile desc = new PluginDescriptionFile(
                "EssentialsX",
                "2.21.0",
                "com.earth2me.essentials.Essentials",
                "1.21");
        samplePlugin.init(desc, customServer);
        assertEquals("EssentialsX", samplePlugin.getName());
        assertEquals("EssentialsX v2.21.0", samplePlugin.getDescription().getFullName());

        samplePlugin.onLoad();
        samplePlugin.saveDefaultConfig();
        assertTrue(samplePlugin.getConfig().getBoolean("plugin.initialized"));

        PluginManager pm = Bukkit.getPluginManager();
        pm.enablePlugin(samplePlugin);
        assertTrue(samplePlugin.isEnabled());
        assertTrue(pm.isPluginEnabled("EssentialsX"));
        assertEquals(samplePlugin, pm.getPlugin("essentialsx"));

        pm.disablePlugin(samplePlugin);
        assertFalse(samplePlugin.isEnabled());
    }

    @Test
    void testAsmCompiledRealBukkitPluginJarClassLoadsAndReceivesDispatchedEvents()
            throws Exception {
        byte[] pluginClassBytes = generateSyntheticBukkitPluginClass();
        Class<?> pluginClass = MethodHandles.lookup().defineClass(pluginClassBytes);
        Object instance = pluginClass.getDeclaredConstructor().newInstance();
        assertTrue(instance instanceof JavaPlugin);
        assertTrue(instance instanceof Listener);

        JavaPlugin plugin = (JavaPlugin) instance;
        Listener listener = (Listener) instance;
        plugin.init(
                new PluginDescriptionFile(
                        "SyntheticVault",
                        "1.5.0",
                        pluginClass.getName(),
                        "1.20"),
                Bukkit.getServer());

        PluginManager pm = Bukkit.getPluginManager();
        pm.enablePlugin(plugin);
        assertEquals("ENABLED", System.getProperty("vessel.test.bukkit.plugin.state"));

        pm.registerEvents(listener, plugin);
        pm.callEvent(new Event(false));
        assertEquals("Event", System.getProperty("vessel.test.bukkit.last.event"));

        pm.disablePlugin(plugin);
        assertEquals("DISABLED", System.getProperty("vessel.test.bukkit.plugin.state"));
    }

    private static byte[] generateSyntheticBukkitPluginClass() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(
                Opcodes.V21,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                "org/bukkit/SyntheticVaultPlugin",
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
        enableMv.visitLdcInsn("vessel.test.bukkit.plugin.state");
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

        MethodVisitor disableMv = cw.visitMethod(Opcodes.ACC_PUBLIC, "onDisable", "()V", null, null);
        disableMv.visitCode();
        disableMv.visitLdcInsn("vessel.test.bukkit.plugin.state");
        disableMv.visitLdcInsn("DISABLED");
        disableMv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/lang/System",
                "setProperty",
                "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                false);
        disableMv.visitInsn(Opcodes.POP);
        disableMv.visitInsn(Opcodes.RETURN);
        disableMv.visitMaxs(2, 1);
        disableMv.visitEnd();

        MethodVisitor onEventMv = cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                "onAnyBukkitEvent",
                "(Lorg/bukkit/event/Event;)V",
                null,
                null);
        onEventMv.visitCode();
        onEventMv.visitLdcInsn("vessel.test.bukkit.last.event");
        onEventMv.visitVarInsn(Opcodes.ALOAD, 1);
        onEventMv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "org/bukkit/event/Event",
                "getEventName",
                "()Ljava/lang/String;",
                false);
        onEventMv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/lang/System",
                "setProperty",
                "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                false);
        onEventMv.visitInsn(Opcodes.POP);
        onEventMv.visitInsn(Opcodes.RETURN);
        onEventMv.visitMaxs(2, 2);
        onEventMv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}
