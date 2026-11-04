package net.minecraftforge.server.permission;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.permission.nodes.PermissionDynamicContext;
import net.minecraftforge.server.permission.nodes.PermissionNode;

/**
 * Global entrypoint for checking permissions in the Forge modding ecosystem.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class PermissionAPI {

    private PermissionAPI() {
    }

    /**
     * Resolves permission status for an online {@link ServerPlayer}.
     *
     * @param player  online server player
     * @param node    permission node to evaluate
     * @param context dynamic contextual arguments
     * @param <T>     permission return type
     * @return resolved permission value
     */
    public static <T> T getPermission(
            ServerPlayer player,
            PermissionNode<T> node,
            PermissionDynamicContext<?>... context) {
        if (node != null && node.getDefaultResolver() != null) {
            return node.getDefaultResolver().resolve(
                    player,
                    player != null ? player.getUUID() : null,
                    context);
        }
        return null;
    }

    /**
     * Resolves permission status for an offline player UUID.
     *
     * @param playerUUID player unique identifier
     * @param node       permission node to evaluate
     * @param context    dynamic contextual arguments
     * @param <T>        permission return type
     * @return resolved permission value
     */
    public static <T> T getOfflinePermission(
            UUID playerUUID,
            PermissionNode<T> node,
            PermissionDynamicContext<?>... context) {
        if (node != null && node.getDefaultResolver() != null) {
            return node.getDefaultResolver().resolve(null, playerUUID, context);
        }
        return null;
    }
}
