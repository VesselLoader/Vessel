package org.bukkit;

import java.util.Objects;
import org.bukkit.block.Block;

/**
 * Canonical Bukkit {@code Chunk} handle representing a 16x16 horizontal column of blocks within a
 * {@link World}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class Chunk {

    private final World world;
    private final int x;
    private final int z;
    private volatile boolean loaded = true;
    private volatile boolean forceLoaded;

    public Chunk(World world, int x, int z) {
        this.world = Objects.requireNonNull(world, "world must not be null");
        this.x = x;
        this.z = z;
    }

    public World getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public boolean load() {
        this.loaded = true;
        return true;
    }

    public boolean unload() {
        this.loaded = false;
        return true;
    }

    public boolean isForceLoaded() {
        return forceLoaded;
    }

    public void setForceLoaded(boolean forced) {
        this.forceLoaded = forced;
    }

    public Block getBlock(int x, int y, int z) {
        return world.getBlockAt((this.x << 4) + (x & 15), y, (this.z << 4) + (z & 15));
    }
}
