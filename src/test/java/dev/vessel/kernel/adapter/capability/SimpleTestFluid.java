package dev.vessel.kernel.adapter.capability;

import dev.vessel.api.capability.VesselFluidHandler;

import java.util.Objects;

/**
 * Top-level test container implementing {@link VesselFluidHandler} for capability verification.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 */
public final class SimpleTestFluid implements VesselFluidHandler {

    private final String fluidType;
    private final long capacity;
    private long amount;

    public SimpleTestFluid(String fluidType, long capacity, long initialAmount) {
        this.fluidType = Objects.requireNonNull(fluidType, "fluidType cannot be null");
        this.capacity = capacity;
        this.amount = initialAmount;
    }

    @Override
    public String getFluidType() {
        return amount > 0L ? fluidType : "";
    }

    @Override
    public long getFluidAmount() {
        return amount;
    }

    @Override
    public long getCapacity() {
        return capacity;
    }

    @Override
    public long fill(String incomingFluid, long fillAmount, boolean simulate) {
        if (incomingFluid.isEmpty() || fillAmount <= 0L) {
            return 0L;
        }
        long space = capacity - amount;
        long accepted = Math.min(fillAmount, space);
        if (!simulate) {
            amount += accepted;
        }
        return accepted;
    }

    @Override
    public long drain(String targetFluid, long maxDrain, boolean simulate) {
        if (amount <= 0L || maxDrain <= 0L) {
            return 0L;
        }
        long drained = Math.min(maxDrain, amount);
        if (!simulate) {
            amount -= drained;
        }
        return drained;
    }
}
