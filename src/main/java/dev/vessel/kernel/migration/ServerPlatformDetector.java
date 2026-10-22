package dev.vessel.kernel.migration;

import java.util.Objects;
import java.util.Set;

/**
 * Inspection scanner that identifies legacy server installations based on
 * characteristic indicator files and configuration layout patterns.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ServerPlatformDetector {

    /**
     * Detects the legacy platform from a set of root relative file and directory names.
     *
     * @param rootFileNames set of file and directory names located in server root
     * @return detected ServerPlatformKind
     */
    public ServerPlatformKind detectFromRootFiles(Set<String> rootFileNames) {
        Objects.requireNonNull(rootFileNames, "rootFileNames cannot be null");

        if (rootFileNames.contains("folia.yml") || rootFileNames.contains("config/folia-global.yml")) {
            return ServerPlatformKind.FOLIA;
        }

        if (rootFileNames.contains("paper.yml")
                || rootFileNames.contains("config/paper-global.yml")
                || rootFileNames.contains("purpur.yml")) {
            return ServerPlatformKind.PAPER;
        }

        if (rootFileNames.contains("spigot.yml")
                || rootFileNames.contains("bukkit.yml")
                || rootFileNames.contains("plugins")) {
            // Check if plugins exists without mods
            if (!rootFileNames.contains("mods")) {
                return ServerPlatformKind.BUKKIT_SPIGOT;
            }
        }

        // Modded loader detection
        for (String name : rootFileNames) {
            String lower = name.toLowerCase();
            if (lower.contains("neoforge") || lower.equals("config/neoforge-server.toml")) {
                return ServerPlatformKind.NEOFORGE;
            }
            if (lower.contains("quilt") || lower.contains(".quilt")) {
                return ServerPlatformKind.QUILT;
            }
            if (lower.contains("fabric-server") || lower.contains("fabric-loader")) {
                return ServerPlatformKind.FABRIC;
            }
            if (lower.contains("forge") || rootFileNames.contains("serverconfig")) {
                return ServerPlatformKind.FORGE;
            }
        }

        // If only mods folder present, default to Forge/Fabric inspection
        if (rootFileNames.contains("mods")) {
            return ServerPlatformKind.FORGE;
        }

        return ServerPlatformKind.VANILLA;
    }
}
