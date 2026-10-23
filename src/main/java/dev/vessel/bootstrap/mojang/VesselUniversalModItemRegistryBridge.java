package dev.vessel.bootstrap.mojang;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Injects all discovered modded items (e.g., Create items, machinery, gears, cogwheels) directly
 * into Minecraft's native {@code BuiltInRegistries.ITEM} registry so that players can tab-complete
 * and receive them via {@code /give @p <modid>:<item>}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.4
 */
public final class VesselUniversalModItemRegistryBridge {

    private final VesselItemIdentifierParser identifierParser = new VesselItemIdentifierParser();
    private final VesselItemInstanceFactory instanceFactory = new VesselItemInstanceFactory();
    private final VesselItemRegistryInjector registryInjector = new VesselItemRegistryInjector();

    public VesselUniversalModItemRegistryBridge() {
    }

    /**
     * Scans {@code mods/} in {@code serverRoot} for item declarations and registers them into
     * Minecraft's native item registry across all eras (1.7.10 – 26.3+).
     *
     * @param tierOneClassLoader active Tier-1 Minecraft classloader
     * @param serverRoot         root directory of the running server
     * @return number of mod items successfully registered
     */
    public int injectAllModItemsIntoRegistry(ClassLoader tierOneClassLoader, Path serverRoot) {
        Objects.requireNonNull(tierOneClassLoader, "tierOneClassLoader must not be null");
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");

        Path modsDir = serverRoot.resolve("mods");
        if (!Files.isDirectory(modsDir)) {
            return 0;
        }

        Set<String> itemIdentifiers = discoverItemIdentifiersFromMods(modsDir);
        if (itemIdentifiers.isEmpty()) {
            return 0;
        }

        try {
            Object itemRegistry = registryInjector.resolveItemRegistry(tierOneClassLoader);
            int registeredCount = 0;
            for (String itemStr : itemIdentifiers) {
                try {
                    Object idObj = identifierParser.parseIdentifier(tierOneClassLoader, itemStr);
                    if (idObj != null && !registryInjector.isAlreadyRegistered(itemRegistry, idObj, itemStr)) {
                        Object itemInstance = instanceFactory.createItemInstance(tierOneClassLoader, idObj, itemStr);
                        if (itemInstance != null) {
                            boolean ok = registryInjector.injectItem(
                                    tierOneClassLoader,
                                    itemRegistry,
                                    idObj,
                                    itemStr,
                                    itemInstance);
                            if (ok) {
                                registeredCount++;
                            }
                        }
                    }
                } catch (Exception ignored) {
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
