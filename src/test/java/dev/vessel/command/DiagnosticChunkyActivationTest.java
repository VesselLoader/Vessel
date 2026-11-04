package dev.vessel.command;

import dev.vessel.adapter.bukkit.PluginCommandAutoRegistrar;
import dev.vessel.adapter.bukkit.PluginJarDescriptorExtractor;
import dev.vessel.adapter.bukkit.PluginYmlCommandDescriptor;
import dev.vessel.bootstrap.mojang.LiveJarBytecodeTransformingClassLoader;
import dev.vessel.kernel.identity.SourceLoaderKind;
import dev.vessel.kernel.identity.VesselModuleIdentifier;
import java.nio.file.Path;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.VesselServer;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

public final class DiagnosticChunkyActivationTest {

    @Test
    void testActivateChunky() {
        Path chunkyJar = Path.of("test-server/plugins/Chunky-Bukkit-1.5.3.jar");
        LiveJarBytecodeTransformingClassLoader loader = new LiveJarBytecodeTransformingClassLoader(
                List.of(chunkyJar),
                getClass().getClassLoader(),
                true,
                true);

        Server server = new VesselServer("VESSEL", "26.3", "26.3");
        Bukkit.setServer(server);

        try {
            Class<?> clazz = Class.forName("org.popcraft.chunky.ChunkyBukkit", true, loader);
            System.out.println("Class loaded: " + clazz);
            System.out.println("is JavaPlugin: " + JavaPlugin.class.isAssignableFrom(clazz));

            JavaPlugin plugin = (JavaPlugin) clazz.getDeclaredConstructor().newInstance();
            System.out.println("Instantiated plugin: " + plugin);

            PluginDescriptionFile desc = new PluginDescriptionFile("Chunky", "1.5.3", clazz.getName(), "26.3");
            plugin.init(desc, server);
            System.out.println("init() completed.");

            try {
                plugin.onLoad();
                System.out.println("onLoad() completed.");
            } catch (Throwable t) {
                System.err.println("onLoad() failed: " + t);
                t.printStackTrace();
            }

            try {
                Bukkit.getPluginManager().enablePlugin(plugin);
                System.out.println("enablePlugin() completed.");
            } catch (Throwable t) {
                System.err.println("enablePlugin() failed: " + t);
                t.printStackTrace();
            }

            PluginJarDescriptorExtractor extractor = new PluginJarDescriptorExtractor();
            List<PluginYmlCommandDescriptor> descriptors = extractor.extractCommandsFromJar(chunkyJar);
            PluginCommandAutoRegistrar registrar = new PluginCommandAutoRegistrar();
            int count = registrar.registerPluginCommands(plugin, descriptors, server.getCommandMap());
            System.out.println("Registered commands count: " + count);
            System.out.println("getCommand('chunky'): " + server.getCommandMap().getCommand("chunky"));
        } catch (Throwable t) {
            System.err.println("Activation completely failed:");
            t.printStackTrace();
        }
    }
}
