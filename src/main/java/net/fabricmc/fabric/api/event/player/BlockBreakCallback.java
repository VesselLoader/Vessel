package net.fabricmc.fabric.api.event.player;

import net.minecraft.core.BlockPos;

/**
 * Canonical Fabric Event Player API callback invoked before a player breaks a block
 * ({@code net.fabricmc.fabric.api.event.player.BlockBreakCallback}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@FunctionalInterface
public interface BlockBreakCallback {

    /**
     * Invoked before a block is broken by a player.
     *
     * @param worldName    world identifier
     * @param playerName   breaking player username
     * @param pos          target block coordinates
     * @param blockStateId canonical block state identifier
     * @return {@code true} to allow the block break to proceed, {@code false} to cancel it
     */
    boolean beforeBlockBreak(String worldName, String playerName, BlockPos pos, String blockStateId);
}
