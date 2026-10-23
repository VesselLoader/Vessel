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
 * @version 1.0.2
 */
public final class VesselUniversalModItemRegistryBridge {

    public VesselUniversalModItemRegistryBridge() {
    }

    /**
     * Scans {@code mods/} in {@code serverRoot} for item declarations and registers them into
     * Minecraft's {@code BuiltInRegistries.ITEM}.
     *
     * @param tierOneClassLoader active Tier-1 Minecraft classloader
     * @param serverRoot         root directory of the running server
     * @return number of mod items successfully registered into BuiltInRegistries.ITEM
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
            Class<?> registriesClass = Class.forName(
                    "net.minecraft.core.registries.BuiltInRegistries",
                    true,
                    tierOneClassLoader);
            Field itemRegistryField = registriesClass.getField("ITEM");
            Object itemRegistry = itemRegistryField.get(null);
            if (itemRegistry == null) {
                return 0;
            }

            Class<?> identifierClass = Class.forName(
                    "net.minecraft.resources.Identifier",
                    true,
                    tierOneClassLoader);
            Method parseIdentifierMethod = identifierClass.getMethod("parse", String.class);

            Class<?> resourceKeyClass = Class.forName(
                    "net.minecraft.resources.ResourceKey",
                    true,
                    tierOneClassLoader);
            Class<?> registriesKeyClass = Class.forName(
                    "net.minecraft.core.registries.Registries",
                    true,
                    tierOneClassLoader);
            Field registriesItemField = registriesKeyClass.getField("ITEM");
            Object registriesItemKey = registriesItemField.get(null);

            Method createResourceKeyMethod = resourceKeyClass.getMethod(
                    "create",
                    resourceKeyClass,
                    identifierClass);

            Class<?> itemClass = Class.forName(
                    "net.minecraft.world.item.Item",
                    true,
                    tierOneClassLoader);
            Class<?> itemPropertiesClass = Class.forName(
                    "net.minecraft.world.item.Item$Properties",
                    true,
                    tierOneClassLoader);
            Constructor<?> itemPropsConstructor = itemPropertiesClass.getDeclaredConstructor();
            itemPropsConstructor.setAccessible(true);
            Method setIdMethod = null;
            try {
                setIdMethod = itemPropertiesClass.getMethod("setId", resourceKeyClass);
            } catch (NoSuchMethodException ignored) {
            }

            Constructor<?> itemConstructor = itemClass.getDeclaredConstructor(itemPropertiesClass);
            itemConstructor.setAccessible(true);

            Class<?> registryInterface = Class.forName(
                    "net.minecraft.core.Registry",
                    true,
                    tierOneClassLoader);
            Method containsKeyMethod = registryInterface.getMethod("containsKey", identifierClass);
            Method registerMethod = registryInterface.getMethod(
                    "register",
                    registryInterface,
                    identifierClass,
                    Object.class);

            // Temporarily unfreeze MappedRegistry to allow dynamic mod item injection
            Field frozenField = findFieldInHierarchy(itemRegistry.getClass(), "frozen");
            if (frozenField != null) {
                frozenField.setAccessible(true);
                frozenField.set(itemRegistry, false);
            }

            int registeredCount = 0;
            for (String itemStr : itemIdentifiers) {
                try {
                    Object idObj = parseIdentifierMethod.invoke(null, itemStr);
                    boolean alreadyPresent = (boolean) containsKeyMethod.invoke(itemRegistry, idObj);
                    if (!alreadyPresent) {
                        Object propsInstance = itemPropsConstructor.newInstance();
                        if (setIdMethod != null && registriesItemKey != null) {
                            Object itemKey = createResourceKeyMethod.invoke(null, registriesItemKey, idObj);
                            setIdMethod.invoke(propsInstance, itemKey);
                        }
                        Object newItemInstance = itemConstructor.newInstance(propsInstance);
                        registerMethod.invoke(null, itemRegistry, idObj, newItemInstance);
                        registeredCount++;
                    }
                } catch (Exception ignored) {
                }
            }

            // Refreeze MappedRegistry
            if (frozenField != null) {
                frozenField.set(itemRegistry, true);
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
                    // Match: assets/<namespace>/models/item/<item_name>.json
                    if (name.startsWith("assets/") && name.contains("/models/item/") && name.endsWith(".json")) {
                        String[] parts = name.split("/");
                        if (parts.length == 5 && "assets".equals(parts[0]) && "models".equals(parts[2]) && "item".equals(parts[3])) {
                            String namespace = parts[1];
                            String filename = parts[4];
                            String itemName = filename.substring(0, filename.length() - 5);
                            result.add(namespace + ":" + itemName);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    private static Field findFieldInHierarchy(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
