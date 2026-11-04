package dev.vessel.bootstrap.mojang;

import dev.vessel.bootstrap.classloading.UnifiedGameAndModClassLoaderBridge;
import dev.vessel.command.VesselServerBridgeRegistry;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Reflectively discovers and resolves mod-specific {@code Item} subclasses and static item
 * field instances from mod classloaders via {@link UnifiedGameAndModClassLoaderBridge}.
 *
 * <p>Enables registering real mod item behavior, capabilities, and tooltips instead of
 * vanilla dummy placeholder items.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselModItemClassResolver {

    private static final String[] ITEM_SUPERCLASS_NAMES = new String[]{
            "net.minecraft.world.item.Item",
            "net.minecraft.item.Item"
    };

    private static final String[] HOLDER_CLASS_SUFFIXES = new String[]{
            ".ModItems",
            ".init.ModItems",
            ".item.ModItems",
            ".items.ModItems",
            ".registry.ModItems",
            ".AllItems",
            ".content.ModItems",
            ".common.item.ModItems"
    };

    public VesselModItemClassResolver() {
    }

    /**
     * Attempts to resolve a custom {@code Item} subclass belonging to the mod defining {@code itemIdentifier}.
     *
     * @param modClassLoader active mod classloader, or {@code null}
     * @param bridge         active classloader bridge, or {@code null}
     * @param itemIdentifier canonical identifier (e.g. {@code "create:wrench"})
     * @return resolved {@code Item} subclass, or {@code null} if no custom mod class was found
     */
    public Class<?> resolveModItemClass(
            ClassLoader modClassLoader,
            UnifiedGameAndModClassLoaderBridge bridge,
            String itemIdentifier) {
        if (itemIdentifier == null || !itemIdentifier.contains(":")) {
            return null;
        }

        int colon = itemIdentifier.indexOf(':');
        String namespace = itemIdentifier.substring(0, colon).trim();
        String path = itemIdentifier.substring(colon + 1).trim();
        String pascal = toPascalCase(path);

        String[] candidateClassNames = new String[]{
                namespace + ".item." + pascal + "Item",
                namespace + ".item." + pascal,
                namespace + ".items." + pascal + "Item",
                namespace + ".items." + pascal,
                namespace + ".content.item." + pascal + "Item",
                namespace + ".common.item." + pascal + "Item",
                namespace + "." + pascal + "Item",
                namespace + "." + pascal
        };

        ClassLoader effectiveLoader = resolveEffectiveClassLoader(modClassLoader, bridge);
        for (String candidate : candidateClassNames) {
            Class<?> found = tryLoadClass(candidate, effectiveLoader, bridge);
            if (found != null && isItemSubclass(found)) {
                return found;
            }
        }

        return null;
    }

    /**
     * Inspects mod item registry holder classes to locate an already-instantiated {@code Item} instance.
     *
     * @param modClassLoader active mod classloader, or {@code null}
     * @param bridge         active classloader bridge, or {@code null}
     * @param itemIdentifier canonical identifier (e.g. {@code "create:wrench"})
     * @return real {@code Item} instance, or {@code null} if not found in static fields
     */
    public Object findStaticItemInstance(
            ClassLoader modClassLoader,
            UnifiedGameAndModClassLoaderBridge bridge,
            String itemIdentifier) {
        if (itemIdentifier == null || !itemIdentifier.contains(":")) {
            return null;
        }

        int colon = itemIdentifier.indexOf(':');
        String namespace = itemIdentifier.substring(0, colon).trim();
        String path = itemIdentifier.substring(colon + 1).trim();

        String upperName = path.toUpperCase().replace('-', '_');
        String camelName = toCamelCase(path);

        ClassLoader effectiveLoader = resolveEffectiveClassLoader(modClassLoader, bridge);

        for (String suffix : HOLDER_CLASS_SUFFIXES) {
            String holderClassName = namespace + suffix;
            Class<?> holderClass = tryLoadClass(holderClassName, effectiveLoader, bridge);
            if (holderClass == null) {
                continue;
            }

            Object instance = extractFieldInstance(holderClass, upperName, camelName);
            if (instance != null) {
                return instance;
            }
        }

        return null;
    }

    private static Object extractFieldInstance(Class<?> holderClass, String upperName, String camelName) {
        Field[] fields = holderClass.getFields();
        for (Field f : fields) {
            if (!Modifier.isStatic(f.getModifiers())) {
                continue;
            }
            String fName = f.getName();
            if (fName.equalsIgnoreCase(upperName) || fName.equals(camelName)) {
                try {
                    f.setAccessible(true);
                    Object val = f.get(null);
                    if (val instanceof Supplier<?> supplier) {
                        val = supplier.get();
                    }
                    if (val != null && isItemInstance(val)) {
                        return val;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private static boolean isItemSubclass(Class<?> candidate) {
        for (String superName : ITEM_SUPERCLASS_NAMES) {
            Class<?> current = candidate;
            while (current != null && current != Object.class) {
                if (superName.equals(current.getName())) {
                    return true;
                }
                current = current.getSuperclass();
            }
        }
        return false;
    }

    private static boolean isItemInstance(Object instance) {
        if (instance == null) {
            return false;
        }
        return isItemSubclass(instance.getClass());
    }

    private static ClassLoader resolveEffectiveClassLoader(
            ClassLoader modClassLoader,
            UnifiedGameAndModClassLoaderBridge bridge) {
        if (modClassLoader != null) {
            return modClassLoader;
        }
        if (bridge != null && bridge.getTierTwoModClassLoader() != null) {
            return bridge.getTierTwoModClassLoader();
        }
        ClassLoader active = VesselServerBridgeRegistry.getActiveModClassLoader();
        if (active != null) {
            return active;
        }
        return VesselModItemClassResolver.class.getClassLoader();
    }

    private static Class<?> tryLoadClass(
            String name,
            ClassLoader classLoader,
            UnifiedGameAndModClassLoaderBridge bridge) {
        if (bridge != null) {
            Class<?> bridged = bridge.tryLoadFromModClassLoader(name);
            if (bridged != null) {
                return bridged;
            }
        }
        if (classLoader != null) {
            try {
                return Class.forName(name, true, classLoader);
            } catch (ClassNotFoundException | LinkageError ignored) {
            }
        }
        return null;
    }

    private static String toPascalCase(String raw) {
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '_' || c == '-') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String toCamelCase(String raw) {
        String pascal = toPascalCase(raw);
        if (pascal.isEmpty()) {
            return "";
        }
        return Character.toLowerCase(pascal.charAt(0)) + pascal.substring(1);
    }
}
