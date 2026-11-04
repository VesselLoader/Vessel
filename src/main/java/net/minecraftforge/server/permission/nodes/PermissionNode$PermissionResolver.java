package net.minecraftforge.server.permission.nodes;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/**
 * Functional resolver callback interface for computing default permission decisions.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @param <T> permission value type
 */
public interface PermissionNode$PermissionResolver<T> {

    /**
     * Resolves the default permission value for the given player.
     *
     * @param player     server player instance (may be {@code null} if resolving offline)
     * @param playerUUID unique identifier of the player
     * @param context    variable dynamic contextual parameters
     * @return resolved permission value
     */
    T resolve(ServerPlayer player, UUID playerUUID, PermissionDynamicContext<?>... context);
}
