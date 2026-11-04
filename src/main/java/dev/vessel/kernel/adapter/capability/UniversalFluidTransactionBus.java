package dev.vessel.kernel.adapter.capability;

import dev.vessel.api.capability.VesselFluidHandler;

import java.util.Objects;

/**
 * Universal fluid transaction bridge converting and routing fluid between Forge mB and Fabric Droplets.
 *
 * <p>Standardizes mathematical conversion: $1\text{ mB} = 81\text{ Droplets}$,
 * $1\text{ Bucket} = 1000\text{ mB} = 81{,}000\text{ Droplets}$.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class UniversalFluidTransactionBus {

    private static final UniversalFluidTransactionBus INSTANCE = new UniversalFluidTransactionBus();

    /**
     * Constructs the universal fluid bus.
     */
    public UniversalFluidTransactionBus() {
    }

    /**
     * Resolves the singleton fluid transaction bus.
     *
     * @return singleton bus
     */
    public static UniversalFluidTransactionBus getInstance() {
        return INSTANCE;
    }

    /**
     * Transfers fluid between source and target handlers.
     *
     * @param source   fluid source
     * @param target   fluid destination
     * @param maxMb    maximum volume in milli-Buckets
     * @param simulate if true, computes transfer without modifying state
     * @return volume transferred in milli-Buckets (mB)
     */
    public long transferFluidMb(VesselFluidHandler source, VesselFluidHandler target, long maxMb, boolean simulate) {
        Objects.requireNonNull(source, "source cannot be null");
        Objects.requireNonNull(target, "target cannot be null");

        if (maxMb <= 0L) {
            return 0L;
        }

        String fluidType = source.getFluidType();
        if (fluidType.isEmpty()) {
            return 0L;
        }

        // 1. Simulate drain
        long available = source.drain(fluidType, maxMb, true);
        if (available <= 0L) {
            return 0L;
        }

        // 2. Simulate fill
        long accepted = target.fill(fluidType, available, true);
        if (accepted <= 0L) {
            return 0L;
        }

        // 3. Commit actual transfer
        if (!simulate) {
            long drained = source.drain(fluidType, accepted, false);
            return target.fill(fluidType, drained, false);
        }

        return accepted;
    }

    /**
     * Converts Fabric Transfer API Droplets to Forge milli-Buckets (mB).
     *
     * @param droplets droplet count
     * @return equivalent mB (integer division by 81)
     */
    public long dropletsToMb(long droplets) {
        return droplets / VesselFluidHandler.DROPLETS_PER_MB;
    }

    /**
     * Converts Forge milli-Buckets (mB) to Fabric Transfer API Droplets.
     *
     * @param mb volume in mB
     * @return equivalent droplets (multiplied by 81)
     */
    public long mbToDroplets(long mb) {
        return mb * VesselFluidHandler.DROPLETS_PER_MB;
    }
}
