package dev.vessel.kernel.adapter.capability;

import dev.vessel.api.capability.VesselEnergy;

/**
 * Top-level test container implementing {@link VesselEnergy} for capability verification.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 */
public final class SimpleTestEnergy implements VesselEnergy {

    private final long capacity;
    private long stored;

    public SimpleTestEnergy(long capacity, long initialStored) {
        this.capacity = capacity;
        this.stored = initialStored;
    }

    @Override
    public long getEnergyStored() {
        return stored;
    }

    @Override
    public long getMaxEnergyStored() {
        return capacity;
    }

    @Override
    public long receiveEnergy(long maxReceive, boolean simulate) {
        long space = capacity - stored;
        long accepted = Math.min(maxReceive, space);
        if (!simulate) {
            stored += accepted;
        }
        return accepted;
    }

    @Override
    public long extractEnergy(long maxExtract, boolean simulate) {
        long available = Math.min(maxExtract, stored);
        if (!simulate) {
            stored -= available;
        }
        return available;
    }
}
