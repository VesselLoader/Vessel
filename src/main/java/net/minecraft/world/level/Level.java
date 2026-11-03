package net.minecraft.world.level;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Base Minecraft Level contract ({@code net.minecraft.world.level.Level}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class Level {

    private final ResourceLocation dimension;
    private final Map<BlockPos, BlockState> blocks = new ConcurrentHashMap<>();
    private final Map<BlockPos, BlockEntity> blockEntities = new ConcurrentHashMap<>();
    private volatile long gameTime;
    private volatile long dayTime;

    /**
     * Constructs a new {@code Level} for the given dimension identifier.
     *
     * @param dimension dimension resource location
     */
    public Level(ResourceLocation dimension) {
        this.dimension = Objects.requireNonNull(dimension, "dimension must not be null");
    }

    public ResourceLocation dimension() {
        return dimension;
    }

    public BlockState getBlockState(BlockPos pos) {
        if (pos == null) {
            return new BlockState("minecraft:air");
        }
        return blocks.getOrDefault(pos, new BlockState("minecraft:air"));
    }

    public boolean setBlockState(BlockPos pos, BlockState state) {
        if (pos == null || state == null) {
            return false;
        }
        blocks.put(pos, state);
        return true;
    }

    public BlockEntity getBlockEntity(BlockPos pos) {
        return pos != null ? blockEntities.get(pos) : null;
    }

    public void setBlockEntity(BlockPos pos, BlockEntity entity) {
        if (pos != null && entity != null) {
            blockEntities.put(pos, entity);
        }
    }

    public long getGameTime() {
        return gameTime;
    }

    public void setGameTime(long time) {
        this.gameTime = time;
    }

    public long getDayTime() {
        return dayTime;
    }

    public void setDayTime(long time) {
        this.dayTime = time;
    }
}
