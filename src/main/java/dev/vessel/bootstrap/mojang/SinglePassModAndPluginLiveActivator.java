package dev.vessel.bootstrap.mojang;

import dev.vessel.adapter.bukkit.PluginCommandAutoRegistrar;
import dev.vessel.adapter.bukkit.PluginJarDescriptorExtractor;
import dev.vessel.adapter.bukkit.PluginYmlCommandDescriptor;
import dev.vessel.adapter.fabric.FabricLoaderShim;
import dev.vessel.adapter.fabric.FabricModContainerShim;
import dev.vessel.kernel.compat.era.ForwardVersionModRejectedException;
import dev.vessel.kernel.compat.era.ForwardVersionModRejectionGuard;
import dev.vessel.kernel.event.orchestration.ForgeAndNeoForgeLifecycleSequenceDispatcher;
import dev.vessel.kernel.identity.SourceLoaderKind;
import dev.vessel.kernel.identity.VesselModuleIdentifier;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.VesselServer;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
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
 * @version 1.0.15
 */
public final class SinglePassModAndPluginLiveActivator {

    private final ForwardVersionModRejectionGuard forwardVersionGuard =
            new ForwardVersionModRejectionGuard();
    private final FastJarEntrypointHeaderScanner entrypointScanner =
            new FastJarEntrypointHeaderScanner();
    private final NestedModJarUnpacker nestedJarUnpacker =
            new NestedModJarUnpacker();
    private final ForgeAndNeoForgeLifecycleSequenceDispatcher forgeLifecycleDispatcher =
            new ForgeAndNeoForgeLifecycleSequenceDispatcher();
    private final PluginJarDescriptorExtractor descriptorExtractor =
            new PluginJarDescriptorExtractor();
    private final PluginCommandAutoRegistrar commandAutoRegistrar =
            new PluginCommandAutoRegistrar();
    private final dev.vessel.adapter.forge.ForgeEventBusSubscriberScanner subscriberScanner =
            new dev.vessel.adapter.forge.ForgeEventBusSubscriberScanner();
    private final Map<String, String> rejectedForwardVersionModules = new LinkedHashMap<>();
    private volatile LiveJarBytecodeTransformingClassLoader activeTransformingClassLoader;

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

        String normalizedBukkitVersion = activeMcVersion != null ? activeMcVersion : "1.21.4";
        if (!normalizedBukkitVersion.contains("-")) {
            long dotCount = normalizedBukkitVersion.chars().filter(c -> c == '.').count();
            if (dotCount < 2) {
                normalizedBukkitVersion = normalizedBukkitVersion + ".0";
            }
            normalizedBukkitVersion = normalizedBukkitVersion + "-R0.1-SNAPSHOT";
        }
        Server liveBukkitServer = new VesselServer(
                "VESSEL",
                normalizedBukkitVersion,
                normalizedBukkitVersion);
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
        // Keep activeTransformingClassLoader open for the server lifetime so mod entities, items,
        // blocks, and event listeners can be loaded on demand.
        int activatedCount = 0;
        try {
            List<Path> expandedJarPaths = nestedJarUnpacker.unpackNestedJars(
                    jarPaths,
                    Path.of("cache", "nested-jars"));
            LiveJarBytecodeTransformingClassLoader transformingLoader =
                    new LiveJarBytecodeTransformingClassLoader(
                            expandedJarPaths,
                            parentClassLoader != null ? parentClassLoader : SinglePassModAndPluginLiveActivator.class.getClassLoader(),
                            customFlags.isUniversalCompatEnabled(),
                            customFlags.isDeepDebugEnabled());
            activeTransformingClassLoader = transformingLoader;
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
            Bukkit.getPluginManager().callEvent(new ServerLoadEvent(ServerLoadEvent.STARTUP_TYPE));
        } catch (Exception e) {
            if (customFlags.isDeepDebugEnabled()) {
                e.printStackTrace();
            }
        }
        return activatedCount;
    }

    /**
     * Returns the active {@link LiveJarBytecodeTransformingClassLoader} hosting the active mods and
     * plugins.
     *
     * @return active transforming classloader, or {@code null} if no mods are loaded
     */
    public LiveJarBytecodeTransformingClassLoader getActiveTransformingClassLoader() {
        return activeTransformingClassLoader;
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

    /**
     * Activates a single pre-loaded mod or plugin class directly using the live multi-ecosystem
     * activation pipeline.
     *
     * @param clazz            loaded candidate class
     * @param moduleId         canonical module identifier
     * @param activeMcVersion  active Minecraft server version
     * @param liveBukkitServer active Bukkit server instance
     * @return {@code true} if the class matched and activated at least one mod or plugin contract
     * @throws Exception if instantiation or entrypoint invocation fails
     */
    public boolean activateModOrPluginClassDirectly(
            Class<?> clazz,
            VesselModuleIdentifier moduleId,
            String activeMcVersion,
            Server liveBukkitServer) throws Exception {
        return activateIfModOrPluginClass(clazz, moduleId, activeMcVersion, liveBukkitServer, null);
    }

    private int activateJarEntrypoints(
            Path jarPath,
            VesselModuleIdentifier moduleId,
            String activeMcVersion,
            Server liveBukkitServer,
            ClassLoader transformingLoader,
            boolean lenientLoad) {
        int count = 0;
        List<String> candidateClasses = entrypointScanner.scanCandidateEntrypointClassNames(jarPath);
        for (String className : candidateClasses) {
            try {
                Class<?> clazz = Class.forName(className, true, transformingLoader);
                int modifiers = clazz.getModifiers();
                if (clazz.isInterface()
                        || Modifier.isAbstract(modifiers)
                        || clazz.isAnnotation()
                        || clazz.isEnum()) {
                    continue;
                }
                if (activateIfModOrPluginClass(clazz, moduleId, activeMcVersion, liveBukkitServer, jarPath)) {
                    count++;
                }
            } catch (Throwable t) {
                if (!lenientLoad) {
                    throw new IllegalStateException(
                            "Failed to activate class " + className + " in " + jarPath, t);
                }
            }
        }
        if (moduleId.loader() == dev.vessel.kernel.identity.SourceLoaderKind.FORGE) {
            subscriberScanner.scanAndRegisterSubscribers(candidateClasses, transformingLoader);
        }
        if (count == 0 && lenientLoad && entrypointScanner.containsExecutableBytecodeOrMixins(jarPath)) {
            if (isBukkitFamilyLoader(moduleId.loader())) {
                JavaPlugin proxyPlugin = new JavaPlugin();
                PluginDescriptionFile description = new PluginDescriptionFile(
                        moduleId.namespace(),
                        moduleId.version(),
                        candidateClasses.isEmpty() ? moduleId.namespace() : candidateClasses.get(0),
                        activeMcVersion);
                proxyPlugin.init(description, liveBukkitServer);
                Bukkit.getPluginManager().enablePlugin(proxyPlugin);
                List<PluginYmlCommandDescriptor> descriptors =
                        descriptorExtractor.extractCommandsFromJar(jarPath);
                commandAutoRegistrar.registerPluginCommands(
                        proxyPlugin,
                        descriptors,
                        liveBukkitServer.getCommandMap());
            }
            count = 1;
        }
        return count;
    }

    private static boolean isBukkitFamilyLoader(SourceLoaderKind loaderKind) {
        return loaderKind == SourceLoaderKind.BUKKIT
                || loaderKind == SourceLoaderKind.SPIGOT
                || loaderKind == SourceLoaderKind.PAPER
                || loaderKind == SourceLoaderKind.FOLIA;
    }

    private boolean activateIfModOrPluginClass(
            Class<?> clazz,
            VesselModuleIdentifier moduleId,
            String activeMcVersion,
            Server liveBukkitServer,
            Path jarPath) throws Exception {
        boolean activated = false;

        // A. Fabric ModInitializer & DedicatedServerModInitializer
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

        // A2. Quilt QSL ModInitializer & DedicatedServerModInitializer
        boolean isQuiltMain = org.quiltmc.qsl.base.api.entrypoint.ModInitializer.class.isAssignableFrom(clazz);
        boolean isQuiltServer =
                org.quiltmc.qsl.base.api.entrypoint.server.DedicatedServerModInitializer.class.isAssignableFrom(clazz);
        if (isQuiltMain || isQuiltServer) {
            Optional<net.fabricmc.loader.api.ModContainer> opt =
                    FabricLoader.getInstance().getModContainer(moduleId.namespace());
            net.fabricmc.loader.api.ModContainer quiltContainer = opt.isPresent()
                    ? opt.get()
                    : FabricLoader.getInstance().registerModContainer(
                            new FabricModContainerShim(
                                     moduleId,
                                     Map.of("name", moduleId.namespace()),
                                     Path.of(".")));
            Object instance = clazz.getDeclaredConstructor().newInstance();
            if (isQuiltMain) {
                ((org.quiltmc.qsl.base.api.entrypoint.ModInitializer) instance).onInitialize(quiltContainer);
            }
            if (isQuiltServer) {
                ((org.quiltmc.qsl.base.api.entrypoint.server.DedicatedServerModInitializer) instance)
                        .onInitializeServer(quiltContainer);
            }
            activated = true;
        }

        // B. Forge @Mod("modid")
        if (clazz.isAnnotationPresent(net.minecraftforge.fml.common.Mod.class)) {
            net.minecraftforge.fml.common.Mod forgeModAnnotation =
                    clazz.getAnnotation(net.minecraftforge.fml.common.Mod.class);
            String forgeModId = (forgeModAnnotation != null && !forgeModAnnotation.value().isBlank())
                    ? forgeModAnnotation.value()
                    : moduleId.namespace();
            net.minecraftforge.eventbus.api.IEventBus forgeBus =
                    new net.minecraftforge.eventbus.api.VesselEventBus();
            net.minecraftforge.fml.ModLoadingContext.get().setActiveNamespace(forgeModId);
            net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().setActiveModEventBus(forgeBus);
            Object forgeModInstance = instantiateForgeOrNeoForgeMod(
                    clazz,
                    net.minecraftforge.eventbus.api.IEventBus.class,
                    forgeBus);
            forgeBus.register(forgeModInstance);
            forgeLifecycleDispatcher.dispatchForgeModLifecycleSequence(forgeModId, forgeBus);
            activated = true;
        }

        // B2. Forge @Mod.EventBusSubscriber
        if (subscriberScanner.isEventBusSubscriber(clazz)) {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(clazz);
            activated = true;
        }

        // C. NeoForge @Mod("modid")
        if (clazz.isAnnotationPresent(net.neoforged.fml.common.Mod.class)) {
            net.neoforged.fml.common.Mod neoForgeModAnnotation =
                    clazz.getAnnotation(net.neoforged.fml.common.Mod.class);
            String neoForgeModId = (neoForgeModAnnotation != null && !neoForgeModAnnotation.value().isBlank())
                    ? neoForgeModAnnotation.value()
                    : moduleId.namespace();
            net.neoforged.bus.api.IEventBus neoForgeBus =
                    new net.neoforged.bus.api.VesselNeoEventBus();
            net.neoforged.fml.ModContainer neoForgeContainer =
                    new net.neoforged.fml.ModContainer(neoForgeModId, neoForgeBus);
            net.neoforged.fml.ModLoadingContext.get().setActiveContainer(neoForgeContainer);
            Object neoForgeModInstance = instantiateNeoForgeMod(
                    clazz,
                    neoForgeBus,
                    neoForgeContainer);
            neoForgeBus.register(neoForgeModInstance);
            forgeLifecycleDispatcher.dispatchNeoForgeModLifecycleSequence(neoForgeModId, neoForgeBus);
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
            if (jarPath != null) {
                List<PluginYmlCommandDescriptor> descriptors =
                        descriptorExtractor.extractCommandsFromJar(jarPath);
                commandAutoRegistrar.registerPluginCommands(
                        plugin,
                        descriptors,
                        liveBukkitServer.getCommandMap());
            }
            try {
                plugin.onLoad();
            } catch (Throwable t) {
                System.err.println("[VESSEL/ERROR] Failed onLoad for Bukkit plugin: " + plugin.getName());
                t.printStackTrace();
            }
            try {
                Bukkit.getPluginManager().enablePlugin(plugin);
            } catch (Throwable t) {
                System.err.println("[VESSEL/ERROR] Failed to enable Bukkit plugin: " + plugin.getName());
                t.printStackTrace();
            }
            if (Listener.class.isAssignableFrom(clazz)) {
                try {
                    Bukkit.getPluginManager().registerEvents((Listener) plugin, plugin);
                } catch (Throwable ignored) {
                }
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

    private static Object instantiateNeoForgeMod(
            Class<?> clazz,
            net.neoforged.bus.api.IEventBus neoForgeBus,
            net.neoforged.fml.ModContainer neoForgeContainer) throws Exception {
        for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
            if (ctor.getParameterCount() == 1) {
                Class<?> paramType = ctor.getParameterTypes()[0];
                if (paramType.isAssignableFrom(net.neoforged.bus.api.IEventBus.class)) {
                    ctor.setAccessible(true);
                    return ctor.newInstance(neoForgeBus);
                }
                if (paramType.isAssignableFrom(net.neoforged.fml.ModContainer.class)) {
                    ctor.setAccessible(true);
                    return ctor.newInstance(neoForgeContainer);
                }
            }
        }
        Constructor<?> defaultCtor = clazz.getDeclaredConstructor();
        defaultCtor.setAccessible(true);
        return defaultCtor.newInstance();
    }
}
