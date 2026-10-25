package dev.vessel.kernel.architectury;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Native synthetic bridge providing runtime resolution for Architectury API's {@code @ExpectPlatform}
 * annotations, platform environment queries, and cross-platform subsystem routing.
 *
 * <p>Quells: ECO-045.</p>
 * <p>Strictly adheres to Rule 17 (zero inner, nested, or anonymous classes).</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ArchitecturySyntheticBridge {

    /**
     * Canonical suffix appended by Architectury compiler for Fabric implementations.
     */
    public static final String FABRIC_IMPL_SUFFIX = "Impl";

    /**
     * Canonical suffix for Forge implementations.
     */
    public static final String FORGE_IMPL_SUFFIX = "ForgeImpl";

    /**
     * Canonical suffix for NeoForge implementations.
     */
    public static final String NEOFORGE_IMPL_SUFFIX = "NeoForgeImpl";

    /**
     * Default config directory path string.
     */
    public static final String DEFAULT_CONFIG_DIR = "config";

    /**
     * Default game directory path string.
     */
    public static final String DEFAULT_GAME_DIR = ".";

    private volatile ArchitecturyPlatformProvider platformProvider;
    private final Map<String, Method> resolvedMethodCache = new ConcurrentHashMap<>();
    private final Map<String, Object> registeredCustomHandlers = new ConcurrentHashMap<>();

    /**
     * Constructs a synthetic bridge with standard native defaults.
     */
    public ArchitecturySyntheticBridge() {
        this.platformProvider = new DefaultArchitecturyPlatformProvider();
    }

    /**
     * Constructs a synthetic bridge with a specific platform provider.
     *
     * @param platformProvider custom provider
     */
    public ArchitecturySyntheticBridge(ArchitecturyPlatformProvider platformProvider) {
        this.platformProvider = Objects.requireNonNull(platformProvider, "platformProvider cannot be null");
    }

    /**
     * Updates the platform provider.
     *
     * @param platformProvider new provider
     */
    public void setPlatformProvider(ArchitecturyPlatformProvider platformProvider) {
        this.platformProvider = Objects.requireNonNull(platformProvider, "platformProvider cannot be null");
        resolvedMethodCache.clear();
    }

    /**
     * Retrieves current platform kind.
     *
     * @return active platform kind
     */
    public ArchitecturyPlatformKind getPlatformKind() {
        return platformProvider.getPlatformKind();
    }

    /**
     * Checks if a mod is loaded via the active provider.
     *
     * @param modId mod identifier
     * @return true if loaded, false otherwise
     */
    public boolean isModLoaded(String modId) {
        Objects.requireNonNull(modId, "modId cannot be null");
        return platformProvider.isModLoaded(modId);
    }

    /**
     * Retrieves the config directory.
     *
     * @return path to config directory
     */
    public Path getConfigDirectory() {
        return platformProvider.getConfigDirectory();
    }

    /**
     * Retrieves the game root directory.
     *
     * @return path to game root directory
     */
    public Path getGameDirectory() {
        return platformProvider.getGameDirectory();
    }

    /**
     * Resolves the target synthetic method backing an {@code @ExpectPlatform} method invocation.
     *
     * @param originalClass the interface/class declaring the expect-platform hook
     * @param methodName the method name
     * @param parameterTypes parameter signature types
     * @return resolved Method reflection object
     * @throws NoSuchMethodException if the synthetic platform method cannot be located
     */
    public Method resolveExpectPlatformMethod(
            Class<?> originalClass,
            String methodName,
            Class<?>... parameterTypes) throws NoSuchMethodException {
        Objects.requireNonNull(originalClass, "originalClass cannot be null");
        Objects.requireNonNull(methodName, "methodName cannot be null");

        String cacheKey = originalClass.getName() + "#" + methodName;
        Method cached = resolvedMethodCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String suffix = resolveSuffixForPlatform(platformProvider.getPlatformKind());
        String implClassName = originalClass.getName() + suffix;

        try {
            Class<?> implClass = Class.forName(implClassName, true, originalClass.getClassLoader());
            Method method = implClass.getMethod(methodName, parameterTypes);
            method.setAccessible(true);
            resolvedMethodCache.put(cacheKey, method);
            return method;
        } catch (ClassNotFoundException e) {
            // Fallback: Check if originalClass itself contains the method directly
            try {
                Method method = originalClass.getMethod(methodName, parameterTypes);
                method.setAccessible(true);
                resolvedMethodCache.put(cacheKey, method);
                return method;
            } catch (NoSuchMethodException fallbackEx) {
                throw new NoSuchMethodException("Unable to resolve Architectury synthetic method for "
                        + originalClass.getName() + "." + methodName + " on platform "
                        + platformProvider.getPlatformKind());
            }
        }
    }

    /**
     * Registers a custom handler object for routing synthetic hooks.
     *
     * @param key handler identifier
     * @param handler handler implementation
     */
    public void registerCustomHandler(String key, Object handler) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(handler, "handler cannot be null");
        registeredCustomHandlers.put(key, handler);
    }

    /**
     * Retrieves a registered custom handler.
     *
     * @param key handler identifier
     * @return registered handler or null
     */
    public Object getCustomHandler(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        return registeredCustomHandlers.get(key);
    }

    private static String resolveSuffixForPlatform(ArchitecturyPlatformKind kind) {
        if (kind == ArchitecturyPlatformKind.FORGE) {
            return FORGE_IMPL_SUFFIX;
        } else if (kind == ArchitecturyPlatformKind.NEOFORGE) {
            return NEOFORGE_IMPL_SUFFIX;
        } else {
            return FABRIC_IMPL_SUFFIX;
        }
    }
}
