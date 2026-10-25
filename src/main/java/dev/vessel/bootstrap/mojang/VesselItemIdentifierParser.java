package dev.vessel.bootstrap.mojang;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Multi-era reflective parser creating Minecraft identifier and resource location instances across
 * all versions of Minecraft:
 * <ul>
 *   <li>Modern 26.3+: {@code net.minecraft.resources.Identifier}</li>
 *   <li>Modern 1.13 – 1.21.3: {@code net.minecraft.resources.ResourceLocation}</li>
 *   <li>Legacy 1.7.10 – 1.12.2: {@code net.minecraft.util.ResourceLocation}</li>
 * </ul>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes, and zero lambdas.</p>
 *
 * @version 1.0.0
 */
public final class VesselItemIdentifierParser {

    private static final String[] IDENTIFIER_CLASS_NAMES = new String[]{
            "net.minecraft.resources.Identifier",
            "net.minecraft.resources.ResourceLocation",
            "net.minecraft.util.ResourceLocation"
    };

    public VesselItemIdentifierParser() {
    }

    /**
     * Resolves the active identifier or resource location class in the given classloader.
     *
     * @param classLoader active game classloader
     * @return the identifier {@link Class}, or {@code null} if not found
     */
    public Class<?> resolveIdentifierClass(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader must not be null");
        for (String name : IDENTIFIER_CLASS_NAMES) {
            try {
                return Class.forName(name, true, classLoader);
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }

    /**
     * Parses the string identifier (e.g. {@code "create:cogwheel"}) into a native identifier or
     * resource location object using the target classloader.
     *
     * @param classLoader active game classloader
     * @param itemStr     full namespaced identifier string (e.g. {@code "create:cogwheel"})
     * @return the constructed identifier object, or the string itself if reflection fails
     */
    public Object parseIdentifier(ClassLoader classLoader, String itemStr) {
        Objects.requireNonNull(classLoader, "classLoader must not be null");
        Objects.requireNonNull(itemStr, "itemStr must not be null");

        Class<?> idClass = resolveIdentifierClass(classLoader);
        if (idClass == null) {
            return itemStr;
        }

        // 1. Try static parse(String)
        try {
            Method parseMethod = idClass.getMethod("parse", String.class);
            return parseMethod.invoke(null, itemStr);
        } catch (Exception ignored) {
        }

        // 2. Try static tryParse(String)
        try {
            Method tryParseMethod = idClass.getMethod("tryParse", String.class);
            Object result = tryParseMethod.invoke(null, itemStr);
            if (result != null) {
                return result;
            }
        } catch (Exception ignored) {
        }

        // 3. Try Constructor(String)
        try {
            Constructor<?> ctor = idClass.getConstructor(String.class);
            return ctor.newInstance(itemStr);
        } catch (Exception ignored) {
        }

        // 4. Try Constructor(String, String) [namespace, path]
        int colon = itemStr.indexOf(':');
        if (colon > 0) {
            String namespace = itemStr.substring(0, colon);
            String path = itemStr.substring(colon + 1);
            try {
                Constructor<?> ctor = idClass.getConstructor(String.class, String.class);
                return ctor.newInstance(namespace, path);
            } catch (Exception ignored) {
            }
        }

        return itemStr;
    }
}
