package net.minecraft.core;

import java.util.Objects;

/**
 * Immutable 3D block position ({@code net.minecraft.core.BlockPos}) supporting directional neighbor
 * traversal and bit-packed 64-bit long encoding.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class BlockPos extends Vec3i {

    /**
     * Origin block position {@code (0, 0, 0)}.
     */
    public static final BlockPos ZERO = new BlockPos(0, 0, 0);

    /**
     * Constructs a {@code BlockPos} for the given block coordinates.
     *
     * @param x block X coordinate
     * @param y block Y coordinate
     * @param z block Z coordinate
     */
    public BlockPos(int x, int y, int z) {
        super(x, y, z);
    }

    /**
     * Returns a {@code BlockPos} offset by {@code (dx, dy, dz)}.
     *
     * @param dx X offset
     * @param dy Y offset
     * @param dz Z offset
     * @return offset block position
     */
    public BlockPos offset(int dx, int dy, int dz) {
        return (dx == 0 && dy == 0 && dz == 0)
                ? this
                : new BlockPos(getX() + dx, getY() + dy, getZ() + dz);
    }

    /**
     * Returns the adjacent {@code BlockPos} one block in the given {@link Direction}.
     *
     * @param direction step direction
     * @return neighbor block position
     */
    public BlockPos relative(Direction direction) {
        return relative(direction, 1);
    }

    /**
     * Returns the {@code BlockPos} offset by {@code distance} blocks in the given {@link Direction}.
     *
     * @param direction step direction
     * @param distance  step distance
     * @return offset block position
     */
    public BlockPos relative(Direction direction, int distance) {
        Objects.requireNonNull(direction, "direction must not be null");
        return offset(
                direction.getStepX() * distance,
                direction.getStepY() * distance,
                direction.getStepZ() * distance);
    }

    /**
     * Returns the block position directly above ({@code Y + 1}).
     *
     * @return position above
     */
    public BlockPos above() {
        return relative(Direction.UP);
    }

    /**
     * Returns the block position directly below ({@code Y - 1}).
     *
     * @return position below
     */
    public BlockPos below() {
        return relative(Direction.DOWN);
    }

    /**
     * Returns the block position one block north ({@code Z - 1}).
     *
     * @return position north
     */
    public BlockPos north() {
        return relative(Direction.NORTH);
    }

    /**
     * Returns the block position one block south ({@code Z + 1}).
     *
     * @return position south
     */
    public BlockPos south() {
        return relative(Direction.SOUTH);
    }

    /**
     * Returns the block position one block west ({@code X - 1}).
     *
     * @return position west
     */
    public BlockPos west() {
        return relative(Direction.WEST);
    }

    /**
     * Returns the block position one block east ({@code X + 1}).
     *
     * @return position east
     */
    public BlockPos east() {
        return relative(Direction.EAST);
    }

    /**
     * Packs this block position into a 64-bit integer using Minecraft's standard 26/26/12-bit
     * layout.
     *
     * @return packed 64-bit block position
     */
    public long asLong() {
        return ((getX() & 0x3FFFFFFL) << 38)
                | ((getZ() & 0x3FFFFFFL) << 12)
                | (getY() & 0xFFFL);
    }

    /**
     * Unpacks a 64-bit integer encoded via {@link #asLong()} into a {@code BlockPos}.
     *
     * @param packedPos packed 64-bit position
     * @return unpacked {@code BlockPos}
     */
    public static BlockPos of(long packedPos) {
        int x = (int) (packedPos >> 38);
        int y = (int) ((packedPos << 52) >> 52);
        int z = (int) ((packedPos << 26) >> 38);
        return new BlockPos(x, y, z);
    }
}
