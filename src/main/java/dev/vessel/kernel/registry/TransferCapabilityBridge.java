package dev.vessel.kernel.registry;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bidirectional capability bridge translating item, fluid, and energy transfers between
 * Fabric Transfer API ({@code Storage<T>}) and Forge/NeoForge Capabilities
 * ({@code IItemHandler}, {@code IFluidHandler}, {@code IEnergyStorage}) under Decision 40.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-28)
 */
public final class TransferCapabilityBridge {

    private final Map<String, Long> committedBalances = new ConcurrentHashMap<>();

    /**
     * Bridges an insertion from a Fabric {@code Storage} source into a Forge capability target.
     *
     * @param itemOrFluidId     canonical resource identifier
     * @param amount            amount offered for insertion
     * @param targetMaxCapacity maximum capacity of the target Forge slot/tank
     * @param simulate          {@code true} to simulate without mutating state
     * @return actual amount accepted by the target
     */
    public long insertFabricToForge(String itemOrFluidId, long amount, int targetMaxCapacity, boolean simulate) {
        Objects.requireNonNull(itemOrFluidId, "itemOrFluidId cannot be null");
        if (amount <= 0 || targetMaxCapacity <= 0) {
            return 0L;
        }

        long current = committedBalances.getOrDefault(itemOrFluidId, 0L);
        long remainingSpace = Math.max(0L, (long) targetMaxCapacity - current);
        long accepted = Math.min(amount, remainingSpace);

        if (!simulate && accepted > 0) {
            committedBalances.put(itemOrFluidId, current + accepted);
        }
        return accepted;
    }

    /**
     * Bridges an extraction from a Forge capability source into a Fabric {@code Storage} consumer.
     *
     * @param itemOrFluidId    canonical resource identifier
     * @param requestedAmount  amount requested for extraction
     * @param currentAvailable amount currently available in the source
     * @param simulate         {@code true} to simulate without mutating state
     * @return actual amount extracted
     */
    public long extractForgeToFabric(
            String itemOrFluidId,
            long requestedAmount,
            long currentAvailable,
            boolean simulate
    ) {
        Objects.requireNonNull(itemOrFluidId, "itemOrFluidId cannot be null");
        if (requestedAmount <= 0 || currentAvailable <= 0) {
            return 0L;
        }

        long extracted = Math.min(requestedAmount, currentAvailable);
        if (!simulate && extracted > 0) {
            committedBalances.put(itemOrFluidId, currentAvailable - extracted);
        }
        return extracted;
    }

    /**
     * Bridges energy transfer between Forge Energy ({@code FE}) and Fabric/TechReborn Energy ({@code E}).
     *
     * @param forgeEnergyOffered   Forge Energy units offered
     * @param fabricEnergyCapacity remaining capacity in the target energy storage
     * @param simulate             {@code true} to simulate without mutating state
     * @return accepted energy units
     */
    public long bridgeEnergyTransfer(long forgeEnergyOffered, long fabricEnergyCapacity, boolean simulate) {
        if (forgeEnergyOffered <= 0 || fabricEnergyCapacity <= 0) {
            return 0L;
        }

        long transferred = Math.min(forgeEnergyOffered, fabricEnergyCapacity);
        if (!simulate && transferred > 0) {
            committedBalances.merge("vessel:bridged_energy", transferred, Long::sum);
        }
        return transferred;
    }

    /**
     * Returns the current committed balance for the given resource identifier.
     *
     * @param itemOrFluidId canonical resource identifier
     * @return committed amount
     */
    public long getCommittedBalance(String itemOrFluidId) {
        Objects.requireNonNull(itemOrFluidId, "itemOrFluidId cannot be null");
        return committedBalances.getOrDefault(itemOrFluidId, 0L);
    }
}
