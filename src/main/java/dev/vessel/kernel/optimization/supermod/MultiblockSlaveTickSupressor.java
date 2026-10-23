package dev.vessel.kernel.optimization.supermod;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Suppresses redundant per-tick updates on Immersive Engineering and Mekanism multiblock slave
 * blocks so only the master controller block executes logic each tick.
 *
 * @version 1.0.0
 */
public final class MultiblockSlaveTickSupressor {

    private final Set<Long> masterBlocks = ConcurrentHashMap.newKeySet();
    private final Set<Long> slaveBlocks = ConcurrentHashMap.newKeySet();

    /**
     * Registers a formed multiblock structure with its master block and slave constituent positions.
     *
     * @param masterBlockPos      the packed 64-bit master block coordinate
     * @param slaveBlockPositions the list of packed 64-bit slave block coordinates
     */
    public void registerMultiblockStructure(long masterBlockPos, List<Long> slaveBlockPositions) {
        Objects.requireNonNull(slaveBlockPositions, "slaveBlockPositions must not be null");
        masterBlocks.add(masterBlockPos);
        slaveBlocks.remove(masterBlockPos);
        for (Long slavePos : slaveBlockPositions) {
            if (slavePos != null && slavePos != masterBlockPos) {
                slaveBlocks.add(slavePos);
            }
        }
    }

    /**
     * Determines whether a multiblock part should be ticked.
     *
     * @param blockPos the packed 64-bit block coordinate
     * @return {@code false} if the block is a registered multiblock slave, {@code true} for master
     *         or standalone blocks
     */
    public boolean shouldTickMultiblockPart(long blockPos) {
        if (slaveBlocks.contains(blockPos)) {
            return false;
        }
        return true;
    }
}
