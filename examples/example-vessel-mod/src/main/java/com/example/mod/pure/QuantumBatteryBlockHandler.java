package com.example.mod.pure;

import dev.vessel.api.block.BlockHarvestGovernor;
import dev.vessel.api.block.ExplosionResistanceCache;
import dev.vessel.api.world.VesselWorldView;

import java.util.Objects;

/**
 * Demonstrates high-speed, zero-allocation block inspection and modification using VESSEL Super-API.
 *
 * <p>Leverages L1 cache explosion tables (< 1.0 ns lookup) and flat harvest logic to evaluate
 * block properties without instantiating heavy Bukkit BlockData or Forge BlockState proxy graphs.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.2
 * @since 1.0.0
 */
public final class QuantumBatteryBlockHandler {

    private final BlockHarvestGovernor harvestGovernor;
    private final ExplosionResistanceCache explosionCache;
    private final VesselWorldView worldView;

    /**
     * Constructs a new block handler with default harvest governor, explosion cache, and world view.
     */
    public QuantumBatteryBlockHandler() {
        this.harvestGovernor = new BlockHarvestGovernor();
        this.explosionCache = new ExplosionResistanceCache();
        this.worldView = VesselWorldView.getInstance();
    }

    /**
     * Inspects a block at specific coordinates in the world and returns its numeric ID.
     *
     * @param worldHandle target world handle
     * @param x           X coordinate
     * @param y           Y coordinate
     * @param z           Z coordinate
     * @return numeric block ID (< 1.0 ns primitive memory lookup)
     */
    public int inspectBlockId(Object worldHandle, int x, int y, int z) {
        Objects.requireNonNull(worldHandle, "worldHandle cannot be null");
        return worldView.getBlockTypeId(worldHandle, x, y, z);
    }

    /**
     * Checks if a player using a diamond pickaxe can harvest the given block efficiently.
     *
     * @param blockId numeric block ID
     * @return {@code true} if harvestable with pickaxe tier 3+
     */
    public boolean canHarvestWithPickaxe(int blockId) {
        return harvestGovernor.canHarvestBlock(blockId, "pickaxe", 3);
    }

    /**
     * Retrieves the explosion resistance of a block from the ultra-fast L1 primitive array cache.
     *
     * @param blockId numeric block ID
     * @return explosion resistance float value (evaluated in < 1.0 ns)
     */
    public float getFastExplosionResistance(int blockId) {
        return explosionCache.getExplosionResistance(blockId);
    }

    /**
     * Mutates a block type in the world.
     *
     * @param worldHandle target world handle
     * @param x           X coordinate
     * @param y           Y coordinate
     * @param z           Z coordinate
     * @param blockId     new block numeric ID
     */
    public void placeQuantumBlock(Object worldHandle, int x, int y, int z, int blockId) {
        Objects.requireNonNull(worldHandle, "worldHandle cannot be null");
        worldView.setBlockTypeId(worldHandle, x, y, z, blockId);
    }
}
