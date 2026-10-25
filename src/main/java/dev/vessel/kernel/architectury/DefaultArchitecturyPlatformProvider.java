package dev.vessel.kernel.architectury;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link ArchitecturyPlatformProvider} for the VESSEL sovereign runtime.
 *
 * <p>Strictly adheres to Rule 17 (zero inner, nested, or anonymous classes).</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class DefaultArchitecturyPlatformProvider implements ArchitecturyPlatformProvider {

    private final Path configDir;
    private final Path gameDir;
    private final Set<String> loadedMods = ConcurrentHashMap.newKeySet();
    private volatile ArchitecturyPlatformKind platformKind = ArchitecturyPlatformKind.VESSEL_NATIVE;

    /**
     * Constructs a default provider with standard directory paths.
     */
    public DefaultArchitecturyPlatformProvider() {
        this(Paths.get(ArchitecturySyntheticBridge.DEFAULT_CONFIG_DIR),
             Paths.get(ArchitecturySyntheticBridge.DEFAULT_GAME_DIR));
    }

    /**
     * Constructs a default provider with specified directories.
     *
     * @param configDir path to config directory
     * @param gameDir path to game directory
     */
    public DefaultArchitecturyPlatformProvider(Path configDir, Path gameDir) {
        this.configDir = Objects.requireNonNull(configDir, "configDir cannot be null");
        this.gameDir = Objects.requireNonNull(gameDir, "gameDir cannot be null");
    }

    @Override
    public ArchitecturyPlatformKind getPlatformKind() {
        return platformKind;
    }

    /**
     * Updates the platform kind.
     *
     * @param platformKind new platform kind
     */
    public void setPlatformKind(ArchitecturyPlatformKind platformKind) {
        this.platformKind = Objects.requireNonNull(platformKind, "platformKind cannot be null");
    }

    @Override
    public boolean isModLoaded(String modId) {
        Objects.requireNonNull(modId, "modId cannot be null");
        return loadedMods.contains(modId.toLowerCase());
    }

    /**
     * Registers a loaded mod identifier.
     *
     * @param modId mod identifier
     */
    public void addLoadedMod(String modId) {
        Objects.requireNonNull(modId, "modId cannot be null");
        loadedMods.add(modId.toLowerCase());
    }

    /**
     * Unregisters a loaded mod identifier.
     *
     * @param modId mod identifier
     */
    public void removeLoadedMod(String modId) {
        Objects.requireNonNull(modId, "modId cannot be null");
        loadedMods.remove(modId.toLowerCase());
    }

    @Override
    public Path getConfigDirectory() {
        return configDir;
    }

    @Override
    public Path getGameDirectory() {
        return gameDir;
    }
}
