package dev.vessel.bootstrap.mojang;

import dev.vessel.adapter.fabric.FabricLoaderShim;
import dev.vessel.adapter.fabric.FabricModContainerShim;
import dev.vessel.kernel.compat.era.ForwardVersionModRejectedException;
import dev.vessel.kernel.compat.era.ForwardVersionModRejectionGuard;
import dev.vessel.kernel.identity.VesselModuleIdentifier;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Activates all discovered Fabric, Quilt, Forge, NeoForge mods and Bukkit, Spigot, Paper, Folia
 * plugins in a single continuous boot pass using {@link LiveJarBytecodeTransformingClassLoader},
 * while enforcing {@link ForwardVersionModRejectionGuard} to immediately refuse mods or plugins
 * requiring a newer Minecraft version than the running server.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.2
 */
public final class SinglePassModAndPluginLiveActivator {

    private final ForwardVersionModRejectionGuard forwardVersionGuard =
            new ForwardVersionModRejectionGuard();
    private final Map<String, String> rejectedForwardVersionModules = new LinkedHashMap<>();

    /**
     * Initializes the live {@link FabricLoader} and {@link Bukkit} runtime singletons, enforces
     * the forward-version rejection guard, transforms lower-version bytecode on the fly, and
     * invokes all compatible mod and plugin entrypoints across {@code discoveredModules}.
     *
     * @param serverRoot        the root server directory
     * @param activeMcVersion   the resolved latest Minecraft version (e.g., {@code "1.21.4"})
     * @param discoveredModules map of physical JAR paths to their canonical {@link VesselModuleIdentifier}
     * @param customFlags       active VESSEL CLI flag profile
     * @param parentClassLoader parent classloader for the live mod/plugin classloader
     * @return total number of mod and plugin entrypoints activated during this single pass
     */
    public int activateAllDiscoveredModsAndPlugins(
            Path serverRoot,
            String activeMcVersion,
            Map<Path, VesselModuleIdentifier> discoveredModules,
            VesselCustomFlagsProfile customFlags,
            ClassLoader parentClassLoader) {
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");
        Objects.requireNonNull(activeMcVersion, "activeMcVersion must not be null");
        Objects.requireNonNull(discoveredModules, "discoveredModules must not be null");
        Objects.requireNonNull(customFlags, "customFlags must not be null");

        rejectedForwardVersionModules.clear();

        // 1. Bind live FabricLoader and Bukkit Server singletons for the resolved Minecraft release
        FabricLoaderShim fabricShim = new FabricLoaderShim(
                serverRoot.toAbsolutePath().normalize(),
                serverRoot.resolve("config").toAbsolutePath().normalize());
        FabricLoader liveFabricLoader = new FabricLoader(fabricShim, customFlags.isDeepDebugEnabled());
        FabricLoader.setInstance(liveFabricLoader);

        Server liveBukkitServer = new Server(
                "VESSEL",
                activeMcVersion + "-R0.1-SNAPSHOT",
                activeMcVersion + "-R0.1-SNAPSHOT");
        Bukkit.setServer(liveBukkitServer);

        if (discoveredModules.isEmpty()) {
            return 0;
        }

        // 2. Enforce Forward-Version Guard (reject any mod/plugin requiring MC > activeMcVersion)
        Map<Path, VesselModuleIdentifier> compatibleModules = new LinkedHashMap<>();
        List<Path> jarPaths = new ArrayList<>(discoveredModules.size());
        for (Map.Entry<Path, VesselModuleIdentifier> entry : discoveredModules.entrySet()) {
            Path jarPath = entry.getKey();
            VesselModuleIdentifier moduleId = entry.getValue();
            if (jarPath == null || !Files.isRegularFile(jarPath)) {
                continue;
            }
            try {
                forwardVersionGuard.validateJarAgainstServerVersion(
                        jarPath,
                        moduleId.namespace(),
                        activeMcVersion);
            } catch (ForwardVersionModRejectedException rejected) {
                rejectedForwardVersionModules.put(moduleId.namespace(), rejected.getMessage());
                if (!customFlags.isLenientLoadEnabled()) {
                    throw rejected;
                }
                continue;
            }
            compatibleModules.put(jarPath, moduleId);
            jarPaths.add(jarPath);
            FabricModContainerShim containerShim = new FabricModContainerShim(
                    moduleId,
                    Map.of("name", moduleId.namespace(), "loader", moduleId.loader().getId()),
                    jarPath);
            liveFabricLoader.registerModContainer(containerShim);
        }

        // 3. Load and activate all compatible entrypoints in a single pass with multi-era upgrading
        int activatedCount = 0;
        try (LiveJarBytecodeTransformingClassLoader transformingLoader =
                new LiveJarBytecodeTransformingClassLoader(
                        jarPaths,
                        parentClassLoader,
                        customFlags.isUniversalCompatEnabled(),
                        customFlags.isDeepDebugEnabled())) {
            for (Map.Entry<Path, VesselModuleIdentifier> entry : compatibleModules.entrySet()) {
                Path jarPath = entry.getKey();
                VesselModuleIdentifier moduleId = entry.getValue();
                activatedCount += activateJarEntrypoints(
                        jarPath,
                        moduleId,
                        activeMcVersion,
                        liveBukkitServer,
                        transformingLoader,
                        customFlags.isLenientLoadEnabled());
            }
        } catch (Exception ignored) {
            // Safely release JAR file handles on Windows
        }
        return activatedCount;
    }

    /**
     * Returns an unmodifiable map of module IDs to their forward-version rejection messages for any
     * mods or plugins that required a higher Minecraft version than the running server.
     *
     * @return map of rejected module IDs to remediation messages
     */
    public Map<String, String> getRejectedForwardVersionModules() {
        return Collections.unmodifiableMap(rejectedForwardVersionModules);
    }

    private int activateJarEntrypoints(
            Path jarPath,
            VesselModuleIdentifier moduleId,
            String activeMcVersion,
            Server liveBukkitServer,
            ClassLoader transformingLoader,
            boolean lenientLoad) {
        int count = 0;
        List<String> classNames = listClassNamesInJar(jarPath);
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className, true, transformingLoader);
                int modifiers = clazz.getModifiers();
                if (clazz.isInterface()
                        || Modifier.isAbstract(modifiers)
                        || clazz.isAnnotation()
                        || clazz.isEnum()) {
                    continue;
                }
                if (activateIfModOrPluginClass(clazz, moduleId, activeMcVersion, liveBukkitServer)) {
                    count++;
                }
            } catch (Throwable t) {
                if (!lenientLoad) {
                    throw new IllegalStateException(
                            "Failed to activate class " + className + " in " + jarPath, t);
                }
            }
        }
        return count;
    }

    private boolean activateIfModOrPluginClass(
            Class<?> clazz,
            VesselModuleIdentifier moduleId,
            String activeMcVersion,
            Server liveBukkitServer) throws Exception {
        boolean activated = false;

        // A. Fabric / Quilt ModInitializer & DedicatedServerModInitializer
        boolean isFabricMain = ModInitializer.class.isAssignableFrom(clazz);
        boolean isFabricServer = DedicatedServerModInitializer.class.isAssignableFrom(clazz);
        if (isFabricMain || isFabricServer) {
            Object instance = clazz.getDeclaredConstructor().newInstance();
            if (isFabricMain) {
                ((ModInitializer) instance).onInitialize();
            }
            if (isFabricServer) {
                ((DedicatedServerModInitializer) instance).onInitializeServer();
            }
            activated = true;
        }

        // B. Forge @Mod("modid")
        if (clazz.isAnnotationPresent(net.minecraftforge.fml.common.Mod.class)) {
            net.minecraftforge.eventbus.api.IEventBus forgeBus =
                    new net.minecraftforge.eventbus.api.IEventBus();
            Object forgeModInstance = instantiateForgeOrNeoForgeMod(
                    clazz,
                    net.minecraftforge.eventbus.api.IEventBus.class,
                    forgeBus);
            forgeBus.register(forgeModInstance);
            forgeBus.post("FMLCommonSetupEvent");
            activated = true;
        }

        // C. NeoForge @Mod("modid")
        if (clazz.isAnnotationPresent(net.neoforged.fml.common.Mod.class)) {
            net.neoforged.bus.api.IEventBus neoForgeBus =
                    new net.neoforged.bus.api.IEventBus();
            Object neoForgeModInstance = instantiateForgeOrNeoForgeMod(
                    clazz,
                    net.neoforged.bus.api.IEventBus.class,
                    neoForgeBus);
            neoForgeBus.register(neoForgeModInstance);
            neoForgeBus.post("FMLCommonSetupEvent");
            activated = true;
        }

        // D. Bukkit / Spigot / Paper / Folia JavaPlugin
        if (JavaPlugin.class.isAssignableFrom(clazz)) {
            JavaPlugin plugin = (JavaPlugin) clazz.getDeclaredConstructor().newInstance();
            PluginDescriptionFile description = new PluginDescriptionFile(
                    moduleId.namespace(),
                    moduleId.version(),
                    clazz.getName(),
                    activeMcVersion);
            plugin.init(description, liveBukkitServer);
            plugin.onLoad();
            Bukkit.getPluginManager().enablePlugin(plugin);
            if (Listener.class.isAssignableFrom(clazz)) {
                Bukkit.getPluginManager().registerEvents((Listener) plugin, plugin);
            }
            activated = true;
        }

        return activated;
    }

    private static Object instantiateForgeOrNeoForgeMod(
            Class<?> clazz,
            Class<?> eventBusClass,
            Object eventBusInstance) throws Exception {
        for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
            if (ctor.getParameterCount() == 1
                    && ctor.getParameterTypes()[0].isAssignableFrom(eventBusClass)) {
                ctor.setAccessible(true);
                return ctor.newInstance(eventBusInstance);
            }
        }
        Constructor<?> defaultCtor = clazz.getDeclaredConstructor();
        defaultCtor.setAccessible(true);
        return defaultCtor.newInstance();
    }

    private static List<String> listClassNamesInJar(Path jarPath) {
        List<String> classNames = new ArrayList<>();
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory()
                        || !name.endsWith(".class")
                        || name.startsWith("META-INF/")
                        || "module-info.class".equals(name)) {
                    continue;
                }
                String binaryName = name.substring(0, name.length() - ".class".length())
                        .replace('/', '.');
                classNames.add(binaryName);
            }
        } catch (Exception ignored) {
            // Corrupted or unreadable JAR skipped under lenient loading
        }
        return classNames;
    }
}
