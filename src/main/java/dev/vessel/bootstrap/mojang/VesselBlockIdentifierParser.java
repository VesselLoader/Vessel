package dev.vessel.bootstrap.mojang;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Parses namespaced string identifiers (e.g., {@code "create:cogwheel"}) into era-appropriate
 * Minecraft identifier objects ({@code net.minecraft.resources.ResourceLocation} or legacy equivalents).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselBlockIdentifierParser {

    private static final String[] LOCATION_CLASS_NAMES = new String[]{
            "net.minecraft.resources.ResourceLocation",
            "net.minecraft.util.ResourceLocation"
    };

    public VesselBlockIdentifierParser() {
    }

    /**
     * Resolves the Minecraft {@code ResourceLocation} class in the given classloader.
     *
     * @param classLoader active game classloader
     * @return the {@code ResourceLocation} class, or {@code null} if not found
     */
    public Class<?> resolveResourceLocationClass(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader must not be null");
        for (String name : LOCATION_CLASS_NAMES) {
            try {
                return Class.forName(name, true, classLoader);
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }

    /**
     * Parses a namespaced string into a {@code ResourceLocation} instance.
     *
     * @param classLoader active game classloader
     * @param idString    namespaced string (e.g. {@code "create:cogwheel"})
     * @return parsed identifier object, or {@code null} if parsing failed
     */
    public Object parseIdentifier(ClassLoader classLoader, String idString) {
        Objects.requireNonNull(classLoader, "classLoader must not be null");
        if (idString == null || idString.isBlank()) {
            return null;
        }

        Class<?> locClass = resolveResourceLocationClass(classLoader);
        if (locClass == null) {
            return null;
        }

        int colonIndex = idString.indexOf(':');
        String namespace = colonIndex >= 0 ? idString.substring(0, colonIndex).trim() : "minecraft";
        String path = colonIndex >= 0 ? idString.substring(colonIndex + 1).trim() : idString.trim();

        // 1. Try static ResourceLocation.fromNamespaceAndPath(namespace, path) (modern 1.21+ / 26.3)
        try {
            Method m = locClass.getMethod("fromNamespaceAndPath", String.class, String.class);
            return m.invoke(null, namespace, path);
        } catch (Exception ignored) {
        }

        // 2. Try static ResourceLocation.parse(String) (modern 1.21+)
        try {
            Method m = locClass.getMethod("parse", String.class);
            return m.invoke(null, idString);
        } catch (Exception ignored) {
        }

        // 3. Try constructor ResourceLocation(String, String) (1.8 – 1.20.6)
        try {
            Constructor<?> ctor = locClass.getDeclaredConstructor(String.class, String.class);
            ctor.setAccessible(true);
            return ctor.newInstance(namespace, path);
        } catch (Exception ignored) {
        }

        // 4. Try constructor ResourceLocation(String) (1.8 – 1.20.6)
        try {
            Constructor<?> ctor = locClass.getDeclaredConstructor(String.class);
            ctor.setAccessible(true);
            return ctor.newInstance(idString);
        } catch (Exception ignored) {
        }

        return null;
    }
}
