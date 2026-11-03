package net.fabricmc.fabric.api.event.player;

import net.minecraft.core.BlockPos;

/**
 * Canonical Fabric Event Player API handler for player block/item use interactions
 * ({@code net.fabricmc.fabric.api.event.player.UseBlockHandler}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@FunctionalInterface
public interface UseBlockHandler {

    /**
     * Invoked when a player interacts with a block or uses an item.
     *
     * @param playerName player username
     * @param worldName  world identifier
     * @param hand       interaction hand ({@code "MAIN_HAND"} or {@code "OFF_HAND"})
     * @param hitPos     targeted block position
     * @return {@link InteractionResult} indicating whether the interaction is consumed, passed, or failed
     */
    InteractionResult interact(String playerName, String worldName, String hand, BlockPos hitPos);
}
