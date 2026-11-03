package org.quiltmc.loader.api;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.fabricmc.loader.api.EntrypointContainer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.ObjectShare;

/**
 * Canonical Quilt Loader static runtime facade ({@code org.quiltmc.loader.api.QuiltLoader})
 * bridged directly to VESSEL's unified {@link FabricLoader} runtime.
 *
 * <p>Real compiled Quilt mods invoke static methods on {@code QuiltLoader} to query loaded
 * mods, resolve configuration directories, inspect entrypoints, and access {@link ObjectShare}.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class QuiltLoader {

    private QuiltLoader() {
    }

    /**
     * Checks whether a mod with the specified ID is currently loaded.
     *
     * @param id mod identifier
     * @return {@code true} if loaded
     */
    public static boolean isModLoaded(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }

    /**
     * Looks up a loaded {@link ModContainer} by mod identifier.
     *
     * @param id mod identifier
     * @return optional containing the {@link ModContainer} if loaded
     */
    public static Optional<ModContainer> getModContainer(String id) {
        return FabricLoader.getInstance().getModContainer(id);
    }

    /**
     * Returns an unmodifiable collection of all loaded {@link ModContainer} instances.
     *
     * @return collection of loaded mod containers
     */
    public static Collection<ModContainer> getAllMods() {
        return FabricLoader.getInstance().getAllMods();
    }

    /**
     * Returns the active server root game directory.
     *
     * @return game directory path
     */
    public static Path getGameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    /**
     * Returns the active configuration directory.
     *
     * @return configuration directory path
     */
    public static Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    /**
     * Returns whether the runtime is operating in a development environment.
     *
     * @return {@code true} if development environment
     */
    public static boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    /**
     * Returns the global inter-mod {@link ObjectShare} registry.
     *
     * @return shared object registry
     */
    public static ObjectShare getObjectShare() {
        return FabricLoader.getInstance().getObjectShare();
    }

    /**
     * Returns the active runtime {@link MappingResolver}.
     *
     * @return mapping resolver
     */
    public static MappingResolver getMappingResolver() {
        return FabricLoader.getInstance().getMappingResolver();
    }

    /**
     * Retrieves all registered entrypoint instances for the given key and contract type.
     *
     * @param key  entrypoint key (e.g., {@code "main"}, {@code "quilt_loader"})
     * @param type expected entrypoint class token
     * @param <T>  entrypoint type
     * @return immutable list of matching entrypoint instances
     */
    public static <T> List<T> getEntrypoints(String key, Class<T> type) {
        return FabricLoader.getInstance().getEntrypoints(key, type);
    }

    /**
     * Retrieves all registered {@link EntrypointContainer} wrappers for the given key and contract type.
     *
     * @param key  entrypoint key
     * @param type expected entrypoint class token
     * @param <T>  entrypoint type
     * @return immutable list of matching entrypoint containers
     */
    public static <T> List<EntrypointContainer<T>> getEntrypointContainers(String key, Class<T> type) {
        return FabricLoader.getInstance().getEntrypointContainers(key, type);
    }
}
