package org.bukkit.block;

import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Runtime representation of a world block state handle ({@code org.bukkit.block.Block}) on the
 * VESSEL Sovereign Microkernel.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class Block {

    private final World world;
    private final int x;
    private final int y;
    private final int z;
    private volatile String type;

    /**
     * Constructs a new {@code Block} handle at the specified world coordinates.
     *
     * @param world owning world
     * @param x     block X coordinate
     * @param y     block Y coordinate
     * @param z     block Z coordinate
     * @param type  initial canonical block material/type identifier
     */
    public Block(World world, int x, int y, int z, String type) {
        this.world = Objects.requireNonNull(world, "world must not be null");
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = Objects.requireNonNull(type, "type must not be null");
    }

    public World getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    /**
     * Returns a new {@link Location} representing this block's coordinates.
     *
     * @return block location
     */
    public Location getLocation() {
        return new Location(world, x, y, z);
    }

    /**
     * Returns the canonical block material/type identifier (e.g., {@code "minecraft:stone"}).
     *
     * @return block type identifier
     */
    public String getType() {
        return type;
    }

    /**
     * Updates the block material/type identifier at this coordinate.
     *
     * @param type non-null, non-blank block type identifier
     */
    public void setType(String type) {
        Objects.requireNonNull(type, "type must not be null");
        if (type.isBlank()) {
            throw new IllegalArgumentException("type must not be blank");
        }
        this.type = type;
    }
}
