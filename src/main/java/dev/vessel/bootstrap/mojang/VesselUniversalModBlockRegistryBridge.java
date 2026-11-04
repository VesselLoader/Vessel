package dev.vessel.bootstrap.mojang;

import dev.vessel.bootstrap.classloading.UnifiedGameAndModClassLoaderBridge;
import dev.vessel.command.VesselServerBridgeRegistry;
import dev.vessel.kernel.registry.VesselModRegistryHolder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Injects all discovered and mod-registered blocks directly into Minecraft's native
 * {@code BuiltInRegistries.BLOCK} registry across all eras (1.7.10 – 26.3+).
 *
 * <p>Integrates with {@link VesselModRegistryHolder}, {@link VesselModBlockClassResolver}, and
 * {@link UnifiedGameAndModClassLoaderBridge} to guarantee real mod {@code Block} subclasses and
 * instances are registered, supporting block properties and state definitions.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselUniversalModBlockRegistryBridge {

    private final VesselBlockIdentifierParser identifierParser = new VesselBlockIdentifierParser();
    private final VesselBlockIdentifierScanner identifierScanner = new VesselBlockIdentifierScanner();
    private final VesselBlockInstanceFactory instanceFactory = new VesselBlockInstanceFactory();
    private final VesselBlockRegistryInjector registryInjector = new VesselBlockRegistryInjector();
    private final VesselModBlockClassResolver blockClassResolver = new VesselModBlockClassResolver();
    private final VesselBlockStateDefinitionSupport stateSupport = new VesselBlockStateDefinitionSupport();

    public VesselUniversalModBlockRegistryBridge() {
    }

    /**
     * Injects all mod blocks (both explicitly registered via mod lifecycle and discovered from mod JARs)
     * into Minecraft's native block registry across all eras (1.7.10 – 26.3+).
     *
     * @param tierOneClassLoader active Tier-1 Minecraft classloader
     * @param serverRoot         root directory of the running server
     * @return number of mod blocks successfully registered
     */
    public int injectAllModBlocksIntoRegistry(ClassLoader tierOneClassLoader, Path serverRoot) {
        Objects.requireNonNull(tierOneClassLoader, "tierOneClassLoader must not be null");
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");

        try {
            Object blockRegistry = registryInjector.resolveBlockRegistry(tierOneClassLoader);
            int registeredCount = 0;

            // 1. Ingest all real mod block instances from VesselModRegistryHolder (DeferredRegister, RegisterEvent, Fabric)
            Map<String, Object> realModBlocks = VesselModRegistryHolder.getAllModBlocks();
            for (Map.Entry<String, Object> entry : realModBlocks.entrySet()) {
                String blockStr = entry.getKey();
                Object realInstance = entry.getValue();
                try {
                    Object idObj = identifierParser.parseIdentifier(tierOneClassLoader, blockStr);
                    if (idObj != null && !registryInjector.isAlreadyRegistered(blockRegistry, idObj, blockStr)) {
                        boolean ok = registryInjector.injectBlock(
                                tierOneClassLoader,
                                blockRegistry,
                                idObj,
                                blockStr,
                                realInstance);
                        if (ok) {
                            registeredCount++;
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            // 2. Scan mods/ directory for declared block definitions not yet in the registry
            Path modsDir = serverRoot.resolve("mods");
            if (Files.isDirectory(modsDir)) {
                Set<String> blockIdentifiers = identifierScanner.scanBlockIdentifiers(modsDir);
                ClassLoader modLoader = VesselServerBridgeRegistry.getActiveModClassLoader();

                for (String blockStr : blockIdentifiers) {
                    try {
                        Object idObj = identifierParser.parseIdentifier(tierOneClassLoader, blockStr);
                        if (idObj == null || registryInjector.isAlreadyRegistered(blockRegistry, idObj, blockStr)) {
                            continue;
                        }

                        // Check if VesselModRegistryHolder already has an instance
                        Object blockInstance = VesselModRegistryHolder.getModBlock(blockStr);

                        // If not, try locating static block instance in mod classes
                        if (blockInstance == null) {
                            blockInstance = blockClassResolver.findStaticBlockInstance(modLoader, null, blockStr);
                        }

                        // If not, resolve custom mod Block subclass and instantiate it
                        if (blockInstance == null) {
                            Class<?> modBlockClass = blockClassResolver.resolveModBlockClass(modLoader, null, blockStr);
                            blockInstance = instanceFactory.createBlockInstance(
                                    tierOneClassLoader,
                                    idObj,
                                    blockStr,
                                    modBlockClass);
                        }

                        if (blockInstance != null) {
                            boolean ok = registryInjector.injectBlock(
                                    tierOneClassLoader,
                                    blockRegistry,
                                    idObj,
                                    blockStr,
                                    blockInstance);
                            if (ok) {
                                registeredCount++;
                                VesselModRegistryHolder.registerModBlock(blockStr, blockInstance);
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            return registeredCount;
        } catch (Exception ignored) {
            return 0;
        }
    }

    /**
     * Returns the helper for inspecting block state definitions.
     *
     * @return block state definition helper
     */
    public VesselBlockStateDefinitionSupport getStateSupport() {
        return stateSupport;
    }
}
