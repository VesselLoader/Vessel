package com.example.mod.pure;

import dev.vessel.api.energy.ZeroAllocationEnergyStorage;

import java.util.Objects;

/**
 * Demonstrates high-performance, 64-bit primitive energy manipulation using VESSEL Super-API.
 *
 * <p>Provides zero-heap allocation energy transfers, completely replacing heavy Forge IEnergyStorage,
 * NeoForge EnergyStorage, and Fabric EnergyStorage transaction overhead.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 * @since 1.0.0
 */
public final class ZeroAllocEnergyService {

    private final ZeroAllocationEnergyStorage energyStorage;

    /**
     * Constructs an energy service with capacity and transfer rate limits.
     *
     * @param capacity    total energy capacity (RF/FE/E)
     * @param maxTransfer maximum input/output rate per tick
     */
    public ZeroAllocEnergyService(long capacity, long maxTransfer) {
        this.energyStorage = new ZeroAllocationEnergyStorage(capacity, maxTransfer, maxTransfer);
    }

    /**
     * Simulates charging an energy consumer from this storage.
     *
     * @param amount requested charge amount
     * @return actual energy extracted
     */
    public long discharge(long amount) {
        return energyStorage.extractEnergy(amount, false);
    }

    /**
     * Charges energy into this storage.
     *
     * @param amount energy to insert
     * @return actual energy inserted
     */
    public long charge(long amount) {
        return energyStorage.receiveEnergy(amount, false);
    }

    /**
     * Transfers energy from a source storage to a destination storage via primitive 64-bit math.
     *
     * @param source      source storage instance
     * @param target      target storage instance
     * @param maxTransfer maximum transfer quota
     * @return actual energy units transferred with 0 heap allocation
     */
    public long transferBetweenStorages(ZeroAllocationEnergyStorage source, ZeroAllocationEnergyStorage target, long maxTransfer) {
        Objects.requireNonNull(source, "source cannot be null");
        Objects.requireNonNull(target, "target cannot be null");

        long extracted = source.extractEnergy(maxTransfer, true);
        long accepted = target.receiveEnergy(extracted, true);
        long toMove = Math.min(extracted, accepted);

        if (toMove > 0L) {
            source.extractEnergy(toMove, false);
            target.receiveEnergy(toMove, false);
        }
        return toMove;
    }

    /**
     * Returns the current energy level in this local storage.
     *
     * @return stored energy units
     */
    public long getStoredEnergy() {
        return energyStorage.getEnergyStored();
    }
}
