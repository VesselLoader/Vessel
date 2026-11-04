package dev.vessel.bootstrap.mojang;

import dev.vessel.bootstrap.classloading.UnifiedGameAndModClassLoaderBridge;
import dev.vessel.command.VesselServerBridgeRegistry;
import dev.vessel.kernel.registry.VesselModRegistryHolder;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Injects all discovered and mod-registered items directly into Minecraft's native
 * {@code BuiltInRegistries.ITEM} registry so players can use, tab-complete, and receive them.
 *
 * <p>Integrates with {@link VesselModRegistryHolder}, {@link VesselModItemClassResolver}, and
 * {@link UnifiedGameAndModClassLoaderBridge} to guarantee real mod {@code Item} subclasses and
 * instances are registered, preserving custom tooltips, NBT tags, and capabilities.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.5
 */
public final class VesselUniversalModItemRegistryBridge {

    private final VesselItemIdentifierParser identifierParser = new VesselItemIdentifierParser();
    private final VesselItemInstanceFactory instanceFactory = new VesselItemInstanceFactory();
    private final VesselItemRegistryInjector registryInjector = new VesselItemRegistryInjector();
    private final VesselModItemClassResolver itemClassResolver = new VesselModItemClassResolver();

    public VesselUniversalModItemRegistryBridge() {
    }

    /**
     * Injects all mod items (both explicitly registered via mod lifecycle and discovered from mod JARs)
     * into Minecraft's native item registry across all eras (1.7.10 – 26.3+).
     *
     * @param tierOneClassLoader active Tier-1 Minecraft classloader
     * @param serverRoot         root directory of the running server
     * @return number of mod items successfully registered
     */
    public int injectAllModItemsIntoRegistry(ClassLoader tierOneClassLoader, Path serverRoot) {
        Objects.requireNonNull(tierOneClassLoader, "tierOneClassLoader must not be null");
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");

        try {
            Object itemRegistry = registryInjector.resolveItemRegistry(tierOneClassLoader);
            int registeredCount = 0;

            // 1. Ingest all real mod item instances from VesselModRegistryHolder (DeferredRegister, RegisterEvent, Fabric)
            Map<String, Object> realModItems = VesselModRegistryHolder.getAllModItems();
            for (Map.Entry<String, Object> entry : realModItems.entrySet()) {
                String itemStr = entry.getKey();
                Object realInstance = entry.getValue();
                try {
                    Object idObj = identifierParser.parseIdentifier(tierOneClassLoader, itemStr);
                    if (idObj != null && !registryInjector.isAlreadyRegistered(itemRegistry, idObj, itemStr)) {
                        boolean ok = registryInjector.injectItem(
                                tierOneClassLoader,
                                itemRegistry,
                                idObj,
                                itemStr,
                                realInstance);
                        if (ok) {
                            registeredCount++;
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            // 2. Scan mods/ directory for declared item identifiers not yet in the registry
            Path modsDir = serverRoot.resolve("mods");
            if (Files.isDirectory(modsDir)) {
                Set<String> itemIdentifiers = discoverItemIdentifiersFromMods(modsDir);
                ClassLoader modLoader = VesselServerBridgeRegistry.getActiveModClassLoader();

                for (String itemStr : itemIdentifiers) {
                    try {
                        Object idObj = identifierParser.parseIdentifier(tierOneClassLoader, itemStr);
                        if (idObj == null || registryInjector.isAlreadyRegistered(itemRegistry, idObj, itemStr)) {
                            continue;
                        }

                        // Check if VesselModRegistryHolder already has an instance
                        Object itemInstance = VesselModRegistryHolder.getModItem(itemStr);

                        // If not, try locating static item instance in mod classes
                        if (itemInstance == null) {
                            itemInstance = itemClassResolver.findStaticItemInstance(modLoader, null, itemStr);
                        }

                        // If not, resolve custom mod Item subclass and instantiate it
                        if (itemInstance == null) {
                            Class<?> modItemClass = itemClassResolver.resolveModItemClass(modLoader, null, itemStr);
                            itemInstance = instanceFactory.createItemInstance(
                                    tierOneClassLoader,
                                    idObj,
                                    itemStr,
                                    modItemClass);
                        }

                        if (itemInstance != null) {
                            boolean ok = registryInjector.injectItem(
                                    tierOneClassLoader,
                                    itemRegistry,
                                    idObj,
                                    itemStr,
                                    itemInstance);
                            if (ok) {
                                registeredCount++;
                                VesselModRegistryHolder.registerModItem(itemStr, itemInstance);
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

    private static Set<String> discoverItemIdentifiersFromMods(Path modsDir) {
        Set<String> result = new LinkedHashSet<>();
        File[] files = modsDir.toFile().listFiles();
        if (files == null) {
            return result;
        }

        for (File file : files) {
            if (!file.isFile() || !file.getName().toLowerCase().endsWith(".jar")) {
                continue;
            }
            try (JarFile jar = new JarFile(file)) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!name.startsWith("assets/")) {
                        continue;
                    }

                    // 1. Match models/item/: assets/<namespace>/models/item/<subpath>/<item>.json (1.8 – 1.21.3)
                    if (name.endsWith(".json")) {
                        int modelIdx = name.indexOf("/models/item/");
                        if (modelIdx > 7) {
                            String namespace = name.substring(7, modelIdx);
                            String itemPath = name.substring(modelIdx + "/models/item/".length(), name.length() - 5);
                            if (!namespace.isBlank() && !itemPath.isBlank() && !namespace.contains("/")) {
                                result.add(namespace + ":" + itemPath);
                                int lastSlash = itemPath.lastIndexOf('/');
                                if (lastSlash >= 0) {
                                    result.add(namespace + ":" + itemPath.substring(lastSlash + 1));
                                }
                            }
                        }

                        // 2. Match modern items/: assets/<namespace>/items/<subpath>/<item>.json (1.21.4+ / 26.3)
                        int itemsIdx = name.indexOf("/items/");
                        if (itemsIdx > 7 && !name.contains("/textures/")) {
                            String namespace = name.substring(7, itemsIdx);
                            String itemPath = name.substring(itemsIdx + "/items/".length(), name.length() - 5);
                            if (!namespace.isBlank() && !itemPath.isBlank() && !namespace.contains("/")) {
                                result.add(namespace + ":" + itemPath);
                                int lastSlash = itemPath.lastIndexOf('/');
                                if (lastSlash >= 0) {
                                    result.add(namespace + ":" + itemPath.substring(lastSlash + 1));
                                }
                            }
                        }
                    }

                    // 3. Match legacy textures: assets/<namespace>/textures/items/<subpath>/<item>.png (1.7.10 – 1.12.2)
                    if (name.endsWith(".png")) {
                        int texItemsIdx = name.indexOf("/textures/items/");
                        if (texItemsIdx > 7) {
                            String namespace = name.substring(7, texItemsIdx);
                            String itemPath = name.substring(texItemsIdx + "/textures/items/".length(), name.length() - 4);
                            if (!namespace.isBlank() && !itemPath.isBlank() && !namespace.contains("/")) {
                                result.add(namespace + ":" + itemPath);
                                int lastSlash = itemPath.lastIndexOf('/');
                                if (lastSlash >= 0) {
                                    result.add(namespace + ":" + itemPath.substring(lastSlash + 1));
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return result;
    }
}
