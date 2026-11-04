package org.bukkit;

import java.util.Objects;
import org.bukkit.block.Block;

/**
 * Sovereign implementation of Bukkit's {@link Chunk} representing a 16x16 horizontal column of blocks.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class VesselChunk implements Chunk {

    private final World world;
    private final int x;
    private final int z;
    private volatile boolean loaded = true;
    private volatile boolean forceLoaded;

    public VesselChunk(World world, int x, int z) {
        this.world = Objects.requireNonNull(world, "world must not be null");
        this.x = x;
        this.z = z;
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getZ() {
        return z;
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public boolean isGenerated() {
        return true;
    }

    @Override
    public boolean load() {
        this.loaded = true;
        return true;
    }

    @Override
    public boolean load(boolean generate) {
        this.loaded = true;
        return true;
    }

    @Override
    public boolean unload() {
        this.loaded = false;
        return true;
    }

    @Override
    public boolean unload(boolean save) {
        this.loaded = false;
        return true;
    }

    @Override
    public boolean isForceLoaded() {
        return forceLoaded;
    }

    @Override
    public void setForceLoaded(boolean forceLoaded) {
        this.forceLoaded = forceLoaded;
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        int worldX = (this.x << 4) + (x & 0xF);
        int worldZ = (this.z << 4) + (z & 0xF);
        return world.getBlockAt(worldX, y, worldZ);
    }
}
