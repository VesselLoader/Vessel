package dev.vessel.kernel.adapter.capability;

import dev.vessel.api.capability.VesselEnergy;

import java.util.Objects;

/**
 * Universal energy transaction bridge connecting Fabric Energy and Forge/NeoForge IEnergyStorage.
 *
 * <p>Enables zero-allocation bi-directional energy exchange between industrial mods
 * across differing loader ecosystems (e.g. Mekanism, Create, TechReborn, Thermal Expansion).</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class UniversalEnergyTransactionBus {

    private static final UniversalEnergyTransactionBus INSTANCE = new UniversalEnergyTransactionBus();

    /**
     * Constructs the universal energy bus.
     */
    public UniversalEnergyTransactionBus() {
    }

    /**
     * Resolves the singleton energy transaction bus.
     *
     * @return singleton bus
     */
    public static UniversalEnergyTransactionBus getInstance() {
        return INSTANCE;
    }

    /**
     * Transfers energy from a source container to a destination container with simulation support.
     *
     * @param source   energy source
     * @param target   energy destination
     * @param maxTransfer maximum energy units (FE) to transfer
     * @param simulate if true, computes potential transfer without modifying state
     * @return actual energy transferred in FE
     */
    public long transferEnergy(VesselEnergy source, VesselEnergy target, long maxTransfer, boolean simulate) {
        Objects.requireNonNull(source, "source cannot be null");
        Objects.requireNonNull(target, "target cannot be null");

        if (maxTransfer <= 0L || !source.canExtract() || !target.canReceive()) {
            return 0L;
        }

        // 1. Query how much source can provide (simulate)
        long available = source.extractEnergy(maxTransfer, true);
        if (available <= 0L) {
            return 0L;
        }

        // 2. Query how much target can accept (simulate)
        long accepted = target.receiveEnergy(available, true);
        if (accepted <= 0L) {
            return 0L;
        }

        // 3. Commit actual transfer if not simulating
        if (!simulate) {
            long extracted = source.extractEnergy(accepted, false);
            return target.receiveEnergy(extracted, false);
        }

        return accepted;
    }
}
