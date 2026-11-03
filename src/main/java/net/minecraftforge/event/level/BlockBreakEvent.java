package net.minecraftforge.event.level;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;

/**
 * Forge block break gameplay event ({@code net.minecraftforge.event.level.BlockBreakEvent})
 * fired on {@code MinecraftForge.EVENT_BUS} when a player breaks a block.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class BlockBreakEvent extends BlockEvent {

    private final UUID playerUuid;
    private volatile int expToDrop;

    /**
     * Constructs a new {@code BlockBreakEvent} for the specified level, coordinates, block state,
     * breaking player UUID, and experience drop amount.
     *
     * @param levelName    non-null level/dimension identifier
     * @param pos          non-null block coordinates
     * @param blockStateId non-null namespaced block state identifier
     * @param playerUuid   non-null breaking player UUID
     * @param expToDrop    initial experience points to drop
     */
    public BlockBreakEvent(
            String levelName,
            BlockPos pos,
            String blockStateId,
            UUID playerUuid,
            int expToDrop) {
        super(levelName, pos, blockStateId);
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid must not be null");
        this.expToDrop = Math.max(0, expToDrop);
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
     * Returns the experience points scheduled to drop when the block breaks.
     *
     * @return non-negative experience points
     */
    public int getExpToDrop() {
        return expToDrop;
    }

    /**
     * Updates the experience points scheduled to drop when the block breaks.
     *
     * @param exp new experience drop amount (clamped to {@code >= 0})
     */
    public void setExpToDrop(int exp) {
        this.expToDrop = Math.max(0, exp);
    }
}
