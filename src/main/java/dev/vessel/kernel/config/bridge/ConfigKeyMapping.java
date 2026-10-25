package dev.vessel.kernel.config.bridge;

import dev.vessel.kernel.identity.SourceLoaderKind;

import java.util.Objects;

/**
 * Immutable cross-loader configuration key mapping descriptor.
 *
 * @param canonicalPath normalized internal VESSEL key path (e.g. {@code "server.port"})
 * @param forgePath     Forge NightConfig path (e.g. {@code "server.listen_port"})
 * @param fabricPath    Fabric/Quilt JSON5 path (e.g. {@code "serverPort"})
 * @param bukkitPath    Bukkit/Spigot YAML path (e.g. {@code "server-port"})
 * @param defaultValue  optional default value fallback
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record ConfigKeyMapping(
        String canonicalPath,
        String forgePath,
        String fabricPath,
        String bukkitPath,
        Object defaultValue
) {
    public ConfigKeyMapping {
        Objects.requireNonNull(canonicalPath, "canonicalPath cannot be null");
    }

    /**
     * Resolves key path for a specific source loader.
     *
     * @param loader source loader
     * @return loader-specific key path
     */
    public String getPathForLoader(SourceLoaderKind loader) {
        if (loader == null) {
            return canonicalPath;
        }
        return switch (loader) {
            case FORGE, NEOFORGE -> (forgePath != null) ? forgePath : canonicalPath;
            case FABRIC, QUILT -> (fabricPath != null) ? fabricPath : canonicalPath;
            case BUKKIT, SPIGOT, PAPER, FOLIA -> (bukkitPath != null) ? bukkitPath : canonicalPath;
            case VESSEL_NATIVE -> canonicalPath;
        };
    }
}
