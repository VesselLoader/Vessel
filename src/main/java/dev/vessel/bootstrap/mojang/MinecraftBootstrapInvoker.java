package dev.vessel.bootstrap.mojang;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Safely invokes {@code net.minecraft.SharedConstants.tryDetectVersion()} and
 * {@code net.minecraft.server.Bootstrap.bootStrap()} on the Tier-1 Game ClassLoader
 * prior to activating mods and plugins.
 *
 * <p>Initializes all native Minecraft built-in registries (blocks, items, game events, sound events)
 * and sets {@code Bootstrap.isBootstrapped = true}, preventing {@link IllegalArgumentException}
 * ({@code Not bootstrapped}) when mod static initializers query items or registries during entrypoint
 * discovery.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public final class MinecraftBootstrapInvoker {

    private static final String SHARED_CONSTANTS_CLASS_NAME = "net.minecraft.SharedConstants";
    private static final String TRY_DETECT_VERSION_METHOD_NAME = "tryDetectVersion";
    private static final String BOOTSTRAP_CLASS_NAME = "net.minecraft.server.Bootstrap";
    private static final String BOOTSTRAP_METHOD_NAME = "bootStrap";

    /**
     * Triggers Minecraft's game version detection and static registry bootstrap on
     * {@code tierOneClassLoader} if present.
     *
     * @param tierOneClassLoader active Tier-1 classloader containing Mojang server classes
     * @return {@code true} if successfully bootstrapped; {@code false} if bootstrap class is not found
     */
    public boolean invokeMinecraftBootstrapIfPresent(ClassLoader tierOneClassLoader) {
        if (tierOneClassLoader == null) {
            return false;
        }
        try {
            // 1. Detect game version first to initialize SharedConstants.CURRENT_VERSION
            try {
                Class<?> sharedConstantsClass =
                        Class.forName(SHARED_CONSTANTS_CLASS_NAME, true, tierOneClassLoader);
                Method tryDetectVersionMethod =
                        sharedConstantsClass.getMethod(TRY_DETECT_VERSION_METHOD_NAME);
                tryDetectVersionMethod.invoke(null);
            } catch (ClassNotFoundException | NoSuchMethodException ignored) {
                // Pre-1.14 legacy server without tryDetectVersion()
            }

            // 2. Invoke Bootstrap.bootStrap() to populate BuiltInRegistries
            Class<?> bootstrapClass = Class.forName(BOOTSTRAP_CLASS_NAME, true, tierOneClassLoader);
            Method bootStrapMethod = bootstrapClass.getMethod(BOOTSTRAP_METHOD_NAME);
            bootStrapMethod.invoke(null);
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            // Not a modern Mojang bundler with Bootstrap (e.g. monolithic legacy 1.12.2)
            return false;
        } catch (Throwable t) {
            Throwable cause = t.getCause() != null ? t.getCause() : t;
            throw new IllegalStateException("Failed to bootstrap Minecraft registries: " + cause.getMessage(), cause);
        }
    }
}
