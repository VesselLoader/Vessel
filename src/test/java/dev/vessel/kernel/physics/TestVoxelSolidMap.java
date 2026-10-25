package dev.vessel.kernel.physics;

import dev.vessel.kernel.optimization.physics.VoxelSolidPredicate;

import java.util.HashSet;
import java.util.Set;

/**
 * Grid-based solid voxel map for testing ray-tracing collision paths.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class TestVoxelSolidMap implements VoxelSolidPredicate {

    private final Set<Long> solidBlocks = new HashSet<>();

    public void setSolid(int x, int y, int z) {
        long key = (((long) x & 0x3FFFFFFL) << 38) | (((long) y & 0xFFFL) << 26) | ((long) z & 0x3FFFFFFL);
        solidBlocks.add(key);
    }

    @Override
    public boolean isSolid(int x, int y, int z) {
        long key = (((long) x & 0x3FFFFFFL) << 38) | (((long) y & 0xFFFL) << 26) | ((long) z & 0x3FFFFFFL);
        return solidBlocks.contains(key);
    }
}
