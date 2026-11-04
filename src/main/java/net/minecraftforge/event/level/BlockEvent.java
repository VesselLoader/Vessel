package net.minecraftforge.event.level;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraftforge.eventbus.api.Event;

/**
 * Base Forge world block event ({@code net.minecraftforge.event.level.BlockEvent}) exposing the
 * level name, block coordinates, block state identifier, and cancellation state.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.2
 */
public class BlockEvent extends Event {

    private final String levelName;
    private final BlockPos pos;
    private final String blockStateId;

    /**
     * Constructs a default {@code BlockEvent} for compatibility.
     */
    public BlockEvent() {
        this("minecraft:overworld", BlockPos.ZERO, "minecraft:air");
    }

    /**
     * Constructs a new {@code BlockEvent} for the given level, coordinates, and block state.
     *
     * @param levelName    non-null level/dimension identifier
     * @param pos          non-null block position
     * @param blockStateId non-null namespaced block state identifier
     */
    public BlockEvent(String levelName, BlockPos pos, String blockStateId) {
        super();
        this.levelName = Objects.requireNonNull(levelName, "levelName must not be null").trim();
        this.pos = Objects.requireNonNull(pos, "pos must not be null");
        this.blockStateId = Objects.requireNonNull(blockStateId, "blockStateId must not be null").trim();
    }

    @Override
    public boolean isCancelable() {
        return true;
    }

    /**
     * Returns the level/dimension identifier where this block event occurred.
     *
     * @return non-null level name
     */
    public final String getLevelName() {
        return levelName;
    }

    /**
     * Returns the block coordinates of this event.
     *
     * @return non-null block position
     */
    public final BlockPos getPos() {
        return pos;
    }

    /**
     * Returns the namespaced block state identifier involved in this event.
     *
     * @return non-null block state identifier
     */
    public final String getState() {
        return blockStateId;
    }
}
