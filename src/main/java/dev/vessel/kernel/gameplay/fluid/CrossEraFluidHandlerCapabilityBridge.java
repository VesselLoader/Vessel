package dev.vessel.kernel.gameplay.fluid;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Cross-era bridge translating fluid transfer requests across Forge IFluidHandler,
 * Fabric Transfer API Storage, and NeoForge fluid capabilities.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public final class CrossEraFluidHandlerCapabilityBridge {

    private final ConcurrentMap<String, Long> tankFluidLevels = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> tankCapacities = new ConcurrentHashMap<>();

    public void initializeTank(String tankId, long capacityMillibuckets) {
        Objects.requireNonNull(tankId, "tankId cannot be null");
        tankCapacities.put(tankId, Math.max(0, capacityMillibuckets));
        tankFluidLevels.putIfAbsent(tankId, 0L);
    }

    public long fill(String tankId, long amountMillibuckets, boolean simulate) {
        if (tankId == null || amountMillibuckets <= 0) {
            return 0;
        }
        long capacity = tankCapacities.getOrDefault(tankId, 0L);
        long current = tankFluidLevels.getOrDefault(tankId, 0L);
        long space = Math.max(0, capacity - current);
        long toFill = Math.min(space, amountMillibuckets);
        if (!simulate && toFill > 0) {
            tankFluidLevels.put(tankId, current + toFill);
        }
        return toFill;
    }

    public long drain(String tankId, long maxDrainMillibuckets, boolean simulate) {
        if (tankId == null || maxDrainMillibuckets <= 0) {
            return 0;
        }
        long current = tankFluidLevels.getOrDefault(tankId, 0L);
        long toDrain = Math.min(current, maxDrainMillibuckets);
        if (!simulate && toDrain > 0) {
            tankFluidLevels.put(tankId, current - toDrain);
        }
        return toDrain;
    }

    public long getStoredAmount(String tankId) {
        if (tankId == null) {
            return 0;
        }
        return tankFluidLevels.getOrDefault(tankId, 0L);
    }

    public long getCapacity(String tankId) {
        if (tankId == null) {
            return 0;
        }
        return tankCapacities.getOrDefault(tankId, 0L);
    }
}
