package dev.vessel.adapter.bukkit;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified permission bridge shared across Bukkit/Spigot/Paper plugins (LuckPerms, Vault) and
 * Fabric/Forge/NeoForge mods (FTB Ranks, Fabric Permissions API), supporting wildcard permission
 * inheritance (e.g., {@code vessel.*} or {@code *}).
 *
 * @version 1.0.0
 */
public final class VesselPermissionBridge {

    private final Map<UUID, Set<String>> playerPermissions = new ConcurrentHashMap<>();

    /**
     * Grants a permission node to a player across all plugin and mod ecosystems.
     *
     * @param playerUuid     the player's unique identifier
     * @param permissionNode the permission node (e.g., {@code vessel.command.tps})
     */
    public void grantPermission(UUID playerUuid, String permissionNode) {
        Objects.requireNonNull(playerUuid, "playerUuid must not be null");
        Objects.requireNonNull(permissionNode, "permissionNode must not be null");
        String normalized = permissionNode.trim().toLowerCase();
        if (!normalized.isEmpty()) {
            playerPermissions
                    .computeIfAbsent(playerUuid, ignored -> ConcurrentHashMap.newKeySet())
                    .add(normalized);
        }
    }

    /**
     * Evaluates whether a player holds the specified permission node directly or via a parent
     * wildcard node (e.g., {@code vessel.*} or {@code *}).
     *
     * @param playerUuid     the player's unique identifier
     * @param permissionNode the permission node to check
     * @return {@code true} if the player has the permission
     */
    public boolean hasUnifiedPermission(UUID playerUuid, String permissionNode) {
        Objects.requireNonNull(playerUuid, "playerUuid must not be null");
        Objects.requireNonNull(permissionNode, "permissionNode must not be null");
        Set<String> granted = playerPermissions.get(playerUuid);
        if (granted == null || granted.isEmpty()) {
            return false;
        }
        String normalized = permissionNode.trim().toLowerCase();
        if (granted.contains("*") || granted.contains(normalized)) {
            return true;
        }
        int dotIndex = normalized.lastIndexOf('.');
        while (dotIndex > 0) {
            String wildcard = normalized.substring(0, dotIndex) + ".*";
            if (granted.contains(wildcard)) {
                return true;
            }
            dotIndex = normalized.lastIndexOf('.', dotIndex - 1);
        }
        return false;
    }
}
