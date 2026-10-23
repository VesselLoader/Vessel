package dev.vessel.adapter.fabric;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Clean-room ABI shim for {@code net.fabricmc.loader.api.FabricLoader}, providing
 * mod container lookup, scoped game/config directories, and cross-mod {@code ObjectShare}.
 *
 * @version 1.0.0
 */
public final class FabricLoaderShim {

    private final Path gameDir;
    private final Path configDir;
    private final Map<String, FabricModContainerShim> containers = new ConcurrentHashMap<>();
    private final Map<String, Object> objectShare = new ConcurrentHashMap<>();

    /**
     * Creates a new Fabric loader shim rooted at the specified game directory.
     *
     * @param gameDir   the server root directory
     * @param configDir the Fabric-scoped configuration directory
     */
    public FabricLoaderShim(Path gameDir, Path configDir) {
        this.gameDir = Objects.requireNonNull(gameDir, "gameDir must not be null");
        this.configDir = Objects.requireNonNull(configDir, "configDir must not be null");
    }

    /**
     * Registers a loaded Fabric or Quilt mod container with this loader shim.
     *
     * @param container the mod container shim
     */
    public void registerModContainer(FabricModContainerShim container) {
        Objects.requireNonNull(container, "container must not be null");
        String modId = container.getIdentifier().namespace().toLowerCase(Locale.ROOT);
        containers.put(modId, container);
    }

    /**
     * Checks whether a mod with the specified ID is currently loaded.
     *
     * @param modId the mod namespace ID
     * @return {@code true} if the mod container is registered
     */
    public boolean isModLoaded(String modId) {
        if (modId == null || modId.isBlank()) {
            return false;
        }
        return containers.containsKey(modId.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Returns the configuration directory exposed to Fabric mods.
     *
     * @return configuration directory path
     */
    public Path getConfigDir() {
        return configDir;
    }

    /**
     * Returns the root game directory exposed to Fabric mods.
     *
     * @return game directory path
     */
    public Path getGameDir() {
        return gameDir;
    }

    /**
     * Looks up a registered mod container by mod ID.
     *
     * @param modId the mod namespace ID
     * @return optional containing the {@link FabricModContainerShim} if present
     */
    public Optional<FabricModContainerShim> getModContainer(String modId) {
        if (modId == null || modId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(containers.get(modId.trim().toLowerCase(Locale.ROOT)));
    }

    /**
     * Publishes a shared key-value entry into the cross-mod {@code ObjectShare} registry.
     *
     * @param key   shared key ( typically {@code "<modid>:<subkey>"})
     * @param value shared value object
     */
    public void putObjectShare(String key, Object value) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        objectShare.put(key, value);
    }

    /**
     * Retrieves a shared value from the cross-mod {@code ObjectShare} registry.
     *
     * @param key shared key
     * @return the shared value, or {@code null} if not present
     */
    public Object getObjectShare(String key) {
        Objects.requireNonNull(key, "key must not be null");
        return objectShare.get(key);
    }
}
