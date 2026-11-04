package dev.vessel.kernel.adapter.capability;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification test suite for {@link UniversalEnergyTransactionBus}, {@link UniversalFluidTransactionBus},
 * and {@link NeoForgeBlockCapabilityCacheRouter}.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 */
public final class UniversalCapabilityBusTest {

    @Test
    @DisplayName("Verify UniversalEnergyTransactionBus transfers energy with simulation and commit")
    void verifyEnergyBusTransfer() {
        UniversalEnergyTransactionBus bus = UniversalEnergyTransactionBus.getInstance();

        SimpleTestEnergy batteryA = new SimpleTestEnergy(100_000L, 50_000L);
        SimpleTestEnergy batteryB = new SimpleTestEnergy(100_000L, 10_000L);

        // Simulate transfer of 20,000 FE
        long simulated = bus.transferEnergy(batteryA, batteryB, 20_000L, true);
        assertThat(simulated).isEqualTo(20_000L);
        assertThat(batteryA.getEnergyStored()).isEqualTo(50_000L); // Unchanged
        assertThat(batteryB.getEnergyStored()).isEqualTo(10_000L);

        // Real commit transfer
        long committed = bus.transferEnergy(batteryA, batteryB, 20_000L, false);
        assertThat(committed).isEqualTo(20_000L);
        assertThat(batteryA.getEnergyStored()).isEqualTo(30_000L);
        assertThat(batteryB.getEnergyStored()).isEqualTo(30_000L);
    }

    @Test
    @DisplayName("Verify UniversalFluidTransactionBus transfers fluid and computes droplet equivalents")
    void verifyFluidBusTransfer() {
        UniversalFluidTransactionBus bus = UniversalFluidTransactionBus.getInstance();

        SimpleTestFluid tankA = new SimpleTestFluid("minecraft:water", 10_000L, 5_000L);
        SimpleTestFluid tankB = new SimpleTestFluid("minecraft:water", 10_000L, 0L);

        long transferred = bus.transferFluidMb(tankA, tankB, 2_000L, false);
        assertThat(transferred).isEqualTo(2_000L);
        assertThat(tankA.getFluidAmount()).isEqualTo(3_000L);
        assertThat(tankB.getFluidAmount()).isEqualTo(2_000L);

        // Math conversions
        assertThat(bus.mbToDroplets(1000L)).isEqualTo(81_000L);
        assertThat(bus.dropletsToMb(81_000L)).isEqualTo(1000L);
    }

    @Test
    @DisplayName("Verify NeoForgeBlockCapabilityCacheRouter primitive key packing and lookup")
    void verifyBlockCapabilityCacheRouter() {
        NeoForgeBlockCapabilityCacheRouter cache = new NeoForgeBlockCapabilityCacheRouter();

        long packedKey = NeoForgeBlockCapabilityCacheRouter.packCoordinate(100, 64, -250);
        SimpleTestEnergy blockEnergy = new SimpleTestEnergy(50_000L, 25_000L);

        cache.putCapability(packedKey, blockEnergy);

        assertThat(cache.getCapability(packedKey, SimpleTestEnergy.class)).contains(blockEnergy);

        cache.invalidate(packedKey);
        assertThat(cache.getCapability(packedKey, SimpleTestEnergy.class)).isEmpty();
    }
}
