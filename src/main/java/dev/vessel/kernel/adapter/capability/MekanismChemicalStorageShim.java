package dev.vessel.kernel.adapter.capability;

import java.util.Objects;

/**
 * Clean-room simulation shim for Mekanism chemical, gas, infusion, and slurry storage.
 *
 * <p>Enables Mekanism multiblock reactors, gas tanks, and chemical pipes to route gases
 * across regional strands without crashing on missing Mekanism SPI runtime classes.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class MekanismChemicalStorageShim {

    private final String chemicalId;
    private final long capacity;
    private long storedAmount;

    /**
     * Constructs a chemical storage shim.
     *
     * @param chemicalId chemical namespaced identifier (e.g. {@code "mekanism:hydrogen"})
     * @param capacity   maximum chemical capacity
     */
    public MekanismChemicalStorageShim(String chemicalId, long capacity) {
        this.chemicalId = Objects.requireNonNull(chemicalId, "chemicalId cannot be null");
        this.capacity = Math.max(0L, capacity);
        this.storedAmount = 0L;
    }

    /**
     * Resolves the chemical identifier.
     *
     * @return chemical id
     */
    public String getChemicalId() {
        return chemicalId;
    }

    /**
     * Resolves total tank capacity.
     *
     * @return capacity
     */
    public long getCapacity() {
        return capacity;
    }

    /**
     * Resolves current chemical volume.
     *
     * @return stored amount
     */
    public long getStoredAmount() {
        return storedAmount;
    }

    /**
     * Injects chemical into the tank.
     *
     * @param amount   amount to insert
     * @param simulate if true, computes insertion without modifying state
     * @return amount accepted
     */
    public long insertChemical(long amount, boolean simulate) {
        if (amount <= 0L) {
            return 0L;
        }
        long space = capacity - storedAmount;
        long accepted = Math.min(amount, space);
        if (!simulate) {
            storedAmount += accepted;
        }
        return accepted;
    }

    /**
     * Extracts chemical from the tank.
     *
     * @param amount   amount to extract
     * @param simulate if true, computes extraction without modifying state
     * @return amount extracted
     */
    public long extractChemical(long amount, boolean simulate) {
        if (amount <= 0L) {
            return 0L;
        }
        long extracted = Math.min(amount, storedAmount);
        if (!simulate) {
            storedAmount -= extracted;
        }
        return extracted;
    }
}
