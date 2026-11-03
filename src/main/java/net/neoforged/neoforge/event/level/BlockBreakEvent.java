package net.neoforged.neoforge.event.level;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;

/**
 * NeoForge block break gameplay event ({@code net.neoforged.neoforge.event.level.BlockBreakEvent})
 * fired on {@code NeoForge.EVENT_BUS} when a player breaks a block.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class BlockBreakEvent {

    private final String levelName;
    private final BlockPos pos;
    private final String blockStateId;
    private final UUID playerUuid;
    private volatile boolean cancelled;

    /**
     * Constructs a new {@code BlockBreakEvent} for the given level, coordinates, block state, and player.
     *
     * @param levelName    non-null level/dimension identifier
     * @param pos          non-null block coordinates
     * @param blockStateId non-null namespaced block state identifier
     * @param playerUuid   non-null breaking player UUID
     */
    public BlockBreakEvent(
            String levelName,
            BlockPos pos,
            String blockStateId,
            UUID playerUuid) {
        this.levelName = Objects.requireNonNull(levelName, "levelName must not be null").trim();
        this.pos = Objects.requireNonNull(pos, "pos must not be null");
        this.blockStateId = Objects.requireNonNull(blockStateId, "blockStateId must not be null").trim();
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid must not be null");
    }

    /**
     * Returns the level/dimension identifier where this block break occurred.
     *
     * @return non-null level name
     */
    public String getLevelName() {
        return levelName;
    }

    /**
     * Returns the coordinates of the block being broken.
     *
     * @return non-null block position
     */
    public BlockPos getPos() {
        return pos;
    }

    /**
     * Returns the namespaced block state identifier of the block being broken.
     *
     * @return non-null block state identifier
     */
    public String getState() {
        return blockStateId;
    }

    /**
     * Returns the UUID of the player breaking the block.
     *
     * @return non-null player UUID
     */
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    /**
     * Returns {@code true} if this block break event has been cancelled.
     *
     * @return {@code true} if cancelled
     */
    public boolean isCanceled() {
        return cancelled;
    }

    /**
     * Updates the cancellation state of this block break event.
     *
     * @param cancel {@code true} to cancel the block break
     */
    public void setCanceled(boolean cancel) {
        this.cancelled = cancel;
    }
}
