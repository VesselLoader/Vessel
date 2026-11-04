package org.bukkit;

import org.bukkit.block.Block;

/**
 * Standard Bukkit {@code Chunk} contract representing a 16x16 horizontal column of blocks within a
 * {@link World}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.2
 */
public interface Chunk {

    World getWorld();

    int getX();

    int getZ();

    boolean isLoaded();

    boolean isGenerated();

    boolean load();

    boolean load(boolean generate);

    boolean unload();

    boolean unload(boolean save);

    boolean isForceLoaded();

    void setForceLoaded(boolean forceLoaded);

    Block getBlock(int x, int y, int z);
}
