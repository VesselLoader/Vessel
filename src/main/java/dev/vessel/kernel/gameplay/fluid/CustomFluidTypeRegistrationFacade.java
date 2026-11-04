package dev.vessel.kernel.gameplay.fluid;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Facade managing dynamic registration and physical properties of custom modded fluids.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public final class CustomFluidTypeRegistrationFacade {

    private final ConcurrentMap<String, FluidDescriptorRecord> registeredFluids = new ConcurrentHashMap<>();

    public void registerFluid(String fluidId, int viscosity, int density, int temperature, int luminance, boolean canConvertToSource) {
        Objects.requireNonNull(fluidId, "fluidId cannot be null");
        FluidDescriptorRecord record = new FluidDescriptorRecord(fluidId, viscosity, density, temperature, luminance, canConvertToSource);
        registeredFluids.put(fluidId, record);
    }

    public FluidDescriptorRecord getFluidProperties(String fluidId) {
        if (fluidId == null) {
            return null;
        }
        return registeredFluids.get(fluidId);
    }

    public boolean isFluidRegistered(String fluidId) {
        return fluidId != null && registeredFluids.containsKey(fluidId);
    }

    public Set<String> getRegisteredFluidIds() {
        return Set.copyOf(registeredFluids.keySet());
    }

    public int getFluidViscosityOrDefault(String fluidId, int defaultViscosity) {
        FluidDescriptorRecord record = registeredFluids.get(fluidId);
        return record != null ? record.viscosity() : defaultViscosity;
    }
}
