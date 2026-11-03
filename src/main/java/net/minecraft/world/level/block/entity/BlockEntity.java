package net.minecraft.world.level.block.entity;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Minecraft runtime block entity representation
 * ({@code net.minecraft.world.level.block.entity.BlockEntity}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class BlockEntity {

    private final BlockPos pos;
    private final BlockState blockState;
    private volatile CompoundTag nbtData;

    /**
     * Constructs a new {@code BlockEntity} at the specified position and block state.
     *
     * @param pos        non-null block position
     * @param blockState non-null block state
     */
    public BlockEntity(BlockPos pos, BlockState blockState) {
        this.pos = Objects.requireNonNull(pos, "pos must not be null");
        this.blockState = Objects.requireNonNull(blockState, "blockState must not be null");
        this.nbtData = new CompoundTag();
    }

    public BlockPos getBlockPos() {
        return pos;
    }

    public BlockState getBlockState() {
        return blockState;
    }

    public CompoundTag getNbtData() {
        return nbtData;
    }

    public void setNbtData(CompoundTag nbtData) {
        this.nbtData = nbtData != null ? nbtData : new CompoundTag();
    }
}
