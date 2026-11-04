package net.minecraftforge.common.util;

import net.minecraft.core.BlockPos;

/**
 * Snapshot of a block in the world before or during a modification ({@code net.minecraftforge.common.util.BlockSnapshot}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class BlockSnapshot {

    private final Object dimension;
    private final Object level;
    private final BlockPos pos;

    public BlockSnapshot(Object dimension, Object level, BlockPos pos) {
        this.dimension = dimension;
        this.level = level;
        this.pos = pos != null ? pos : BlockPos.ZERO;
    }

    public static BlockSnapshot create(Object dimension, Object level, BlockPos pos) {
        return new BlockSnapshot(dimension, level, pos);
    }

    public boolean restore(boolean force, boolean applyPhysics) {
        return true;
    }

    public BlockPos getPos() {
        return pos;
    }

    public Object getLevel() {
        return level;
    }

    public Object getDimension() {
        return dimension;
    }
}
