package net.fabricmc.loader.api;

import dev.vessel.adapter.fabric.FabricLoaderShim;
import dev.vessel.adapter.fabric.FabricModContainerShim;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Canonical Fabric/Quilt runtime loader singleton ({@code net.fabricmc.loader.api.FabricLoader})
 * bridged directly to VESSEL's {@link FabricLoaderShim}.
 *
 * <p>Real compiled Fabric and Quilt mods invoke {@link #getInstance()} during
 * {@link net.fabricmc.api.ModInitializer#onInitialize()} to query loaded mods, resolve
 * {@link #getConfigDir()}, and inspect {@link ModContainer} metadata.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class FabricLoader {

    private static volatile FabricLoader instance = new FabricLoader(
            new FabricLoaderShim(
                    Paths.get(".").toAbsolutePath().normalize(),
                    Paths.get("config").toAbsolutePath().normalize()),
            false);

    private final FabricLoaderShim underlyingShim;
    private final boolean developmentEnvironment;
    private final Map<String, ModContainer> liveContainers = new ConcurrentHashMap<>();

    /**
     * Constructs a live {@code FabricLoader} backed by the specified {@link FabricLoaderShim}.
     *
     * @param underlyingShim         VESSEL's underlying Fabric loader shim
     * @param developmentEnvironment whether the server is running in a development workspace
     */
    public FabricLoader(FabricLoaderShim underlyingShim, boolean developmentEnvironment) {
        this.underlyingShim = Objects.requireNonNull(underlyingShim, "underlyingShim must not be null");
        this.developmentEnvironment = developmentEnvironment;
    }

    /**
     * Returns the active global {@code FabricLoader} singleton instance.
     *
     * @return non-null {@code FabricLoader} instance
     */
    public static FabricLoader getInstance() {
        return instance;
    }

    /**
     * Binds the active global {@code FabricLoader} singleton instance.
     *
     * @param loader non-null loader instance
     */
    public static void setInstance(FabricLoader loader) {
        instance = Objects.requireNonNull(loader, "loader must not be null");
    }

    /**
     * Registers a mod container with both the live API surface and VESSEL's underlying shim.
     *
     * @param container non-null {@link ModContainer}
     */
    public void registerModContainer(ModContainer container) {
        Objects.requireNonNull(container, "container must not be null");
        underlyingShim.registerModContainer(container.getUnderlyingShim());
        liveContainers.put(container.getModId().toLowerCase(Locale.ROOT), container);
    }

    /**
     * Registers a VESSEL {@link FabricModContainerShim} directly into this live loader.
     *
     * @param shim non-null container shim
     * @return the wrapped {@link ModContainer}
     */
    public ModContainer registerModContainer(FabricModContainerShim shim) {
        Objects.requireNonNull(shim, "shim must not be null");
        ModContainer container = new ModContainer(shim);
        registerModContainer(container);
        return container;
    }

    /**
     * Checks whether a mod with the specified ID is currently loaded.
     *
     * @param id mod ID
     * @return {@code true} if the mod is registered
     */
    public boolean isModLoaded(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        return liveContainers.containsKey(normalized) || underlyingShim.isModLoaded(normalized);
    }

    /**
     * Looks up a loaded {@link ModContainer} by mod ID.
     *
     * @param id mod ID
     * @return optional containing the {@link ModContainer} if present
     */
    public Optional<ModContainer> getModContainer(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        ModContainer existing = liveContainers.get(normalized);
        if (existing != null) {
            return Optional.of(existing);
        }
        Optional<FabricModContainerShim> shimOpt = underlyingShim.getModContainer(normalized);
        if (shimOpt.isPresent()) {
            ModContainer wrapped = new ModContainer(shimOpt.get());
            liveContainers.put(normalized, wrapped);
            return Optional.of(wrapped);
        }
        return Optional.empty();
    }

    /**
     * Returns an unmodifiable collection of all registered {@link ModContainer} instances.
     *
     * @return all loaded mod containers
     */
    public Collection<ModContainer> getAllMods() {
        return Collections.unmodifiableList(new ArrayList<>(liveContainers.values()));
    }

    /**
     * Returns the active server root game directory.
     *
     * @return game directory path
     */
    public Path getGameDir() {
        return underlyingShim.getGameDir();
    }

    /**
     * Returns the active Fabric configuration directory.
     *
     * @return configuration directory path
     */
    public Path getConfigDir() {
        return underlyingShim.getConfigDir();
    }

    /**
     * Returns whether the runtime is operating in a development environment.
     *
     * @return {@code true} if development environment
     */
    public boolean isDevelopmentEnvironment() {
        return developmentEnvironment;
    }

    /**
     * Returns VESSEL's underlying {@link FabricLoaderShim}.
     *
     * @return underlying loader shim
     */
    public FabricLoaderShim getUnderlyingShim() {
        return underlyingShim;
    }
}
