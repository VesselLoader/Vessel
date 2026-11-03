package dev.vessel.bootstrap.mojang;

import java.util.Objects;

/**
 * Filter determining whether a fully-qualified class name belongs to the Minecraft vanilla game or
 * Mojang core library namespaces ({@code net.minecraft.*}, {@code com.mojang.*}) that must be loaded
 * child-first in {@link TierOneGameTransformingClassLoader} to avoid shadowing by kernel-level stubs.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class TierOneGamePackageFilter {

    public static final String NET_MINECRAFT_PREFIX = "net.minecraft.";
    public static final String COM_MOJANG_PREFIX = "com.mojang.";

    /**
     * Checks if the given fully-qualified class name belongs to Minecraft or Mojang game packages.
     *
     * @param className fully-qualified class name to check
     * @return {@code true} if the class is a vanilla game or Mojang library class
     */
    public boolean isGameOrMojangClass(String className) {
        if (className == null || className.isBlank()) {
            return false;
        }
        return className.startsWith(NET_MINECRAFT_PREFIX) || className.startsWith(COM_MOJANG_PREFIX);
    }
}
