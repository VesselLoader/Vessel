package dev.vessel.kernel.optimization.supermod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Throttles PneumaticCraft: Repressurized and Mekanism thermodynamic heat-exchange calculations
 * to run once every {@code N} ticks while scaling the conductivity step by {@code N} to preserve
 * exact physical equilibrium curves.
 *
 * @version 1.0.0
 */
public final class HeatPressureSimulationThrottle {

    private final Map<Long, Float> cachedTemperatures = new ConcurrentHashMap<>();

    /**
     * Computes the throttled thermal transfer temperature for a machine block.
     *
     * @param machinePos            the packed 64-bit machine block coordinate
     * @param currentTemp           the current machine temperature
     * @param targetTemp            the ambient or neighbor target temperature
     * @param conductivity          the per-tick thermal conductivity coefficient
     * @param currentTick           the current monotonic server tick
     * @param throttleIntervalTicks the simulation interval in ticks
     * @return the updated or cached machine temperature
     */
    public float computeThrottledThermalTransfer(
            long machinePos,
            float currentTemp,
            float targetTemp,
            float conductivity,
            long currentTick,
            int throttleIntervalTicks) {
        int interval = Math.max(1, throttleIntervalTicks);
        if ((currentTick % interval) != 0L) {
            return cachedTemperatures.getOrDefault(machinePos, currentTemp);
        }
        float effectiveConductivity = Math.min(1.0f, Math.max(0.0f, conductivity * interval));
        float nextTemp = currentTemp + (targetTemp - currentTemp) * effectiveConductivity;
        cachedTemperatures.put(machinePos, nextTemp);
        return nextTemp;
    }
}
