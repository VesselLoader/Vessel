package com.example.mod.pure;

import dev.vessel.api.VesselAPI;
import dev.vessel.api.block.BlockHarvestGovernor;
import dev.vessel.api.block.ExplosionResistanceCache;
import dev.vessel.api.block.VesselBlock;
import dev.vessel.api.world.VesselWorldView;

import java.util.Objects;

/**
 * Demonstrates high-speed, zero-allocation block inspection and creation using VESSEL Super-API.
 *
 * <p>Leverages {@link VesselAPI#createBlock(String)} for fluent declaration, L1 cache explosion
 * tables (< 1.0 ns lookup), and flat harvest logic to evaluate block properties without instantiating
 * heavy Bukkit BlockData or Forge BlockState proxy graphs.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.4
 * @since 1.0.0
 */
public final class QuantumBatteryBlockHandler {

    private final BlockHarvestGovernor harvestGovernor;
    private final ExplosionResistanceCache explosionCache;
    private final VesselWorldView worldView;
    private final VesselBlock customBlock;

    /**
     * Constructs a new block handler with default harvest governor, explosion cache, and world view.
     */
    public QuantumBatteryBlockHandler() {
        this.harvestGovernor = new BlockHarvestGovernor();
        this.explosionCache = new ExplosionResistanceCache();
        this.worldView = VesselWorldView.getInstance();
        this.customBlock = VesselAPI.createBlock("vesselmod:quantum_battery")
                .hardness(4.5f)
                .resistance(12.0f)
                .luminance(15)
                .sound("metal")
                .requiresTool(true)
                .energyStorage(50_000_000L)
                .drops("vesselmod:quantum_battery", 1, 1)
                .build();
    }

    /**
     * Resolves the custom VesselBlock definition.
     *
     * @return custom block
     */
    public VesselBlock getCustomBlock() {
        return customBlock;
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
     * @return blast resistance (< 1.0 ns flat array lookup)
     */
    public float getExplosionResistance(int blockId) {
        return explosionCache.getExplosionResistance(blockId);
    }
}
