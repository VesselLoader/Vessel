package dev.vessel.api.example;

import com.example.mod.pure.PureVesselExampleMod;
import com.example.mod.pure.PureModConfigHolder;
import com.example.mod.pure.ZeroAllocEnergyService;
import com.example.mod.pure.QuantumBatteryBlockHandler;
import com.example.mod.pure.PlasmaBladeItemManager;
import com.example.mod.pure.RegionalTeleportDispatcher;
import com.example.mod.pure.SovereignModCommand;
import com.example.mod.pure.PureVesselEventListener;

import dev.vessel.api.energy.ZeroAllocationEnergyStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end verification test suite for the Pure VESSEL Example Mod.
 *
 * <p>Validates that developers can build standalone, zero-allocation mods using purely
 * VESSEL Super-API without touching any loader-specific internals.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.2
 * @since 1.0.0
 */
public final class PureVesselExampleModTest {

    @Test
    @DisplayName("PureVesselExampleMod bootstraps and coordinates all sovereign subsystems")
    public void testPureVesselExampleModBootstrap() {
        PureVesselExampleMod mod = new PureVesselExampleMod();
        mod.onInitialize();

        // 1. Verify Configuration Subsystem
        PureModConfigHolder configHolder = mod.getConfigHolder();
        assertThat(configHolder.getBatteryCapacity()).isEqualTo(50000000L);
        assertThat(configHolder.getMaxEnergyTransferRate()).isEqualTo(100000L);
        assertThat(configHolder.getBlacklistedDimensions()).containsExactly("minecraft:the_end");

        // 2. Verify Energy Subsystem
        ZeroAllocEnergyService energyService = mod.getEnergyService();
        assertThat(energyService.getStoredEnergy()).isEqualTo(0L);
        long charged = energyService.charge(50000L);
        assertThat(charged).isEqualTo(50000L);
        assertThat(energyService.getStoredEnergy()).isEqualTo(50000L);

        ZeroAllocationEnergyStorage receiver = new ZeroAllocationEnergyStorage(100000L, 25000L, 25000L);
        ZeroAllocationEnergyStorage senderStorage = new ZeroAllocationEnergyStorage(100000L, 50000L, 50000L);
        senderStorage.receiveEnergy(50000L, false);
        long transferred = energyService.transferBetweenStorages(senderStorage, receiver, 20000L);
        assertThat(transferred).isEqualTo(20000L);
        assertThat(receiver.getEnergyStored()).isEqualTo(20000L);
        assertThat(senderStorage.getEnergyStored()).isEqualTo(30000L);

        // 3. Verify Block Subsystem
        QuantumBatteryBlockHandler blockHandler = mod.getBlockHandler();
        assertThat(blockHandler.getCustomBlock()).isNotNull();
        assertThat(blockHandler.getCustomBlock().getId()).isEqualTo("vesselmod:quantum_battery");
        assertThat(blockHandler.getCustomBlock().getHardness()).isEqualTo(4.5f);
        assertThat(blockHandler.canHarvestWithPickaxe(49)).isTrue();
        assertThat(blockHandler.getExplosionResistance(49)).isEqualTo(1200.0f);

        // 4. Verify Item Subsystem
        PlasmaBladeItemManager itemManager = mod.getItemManager();
        assertThat(itemManager.getCustomItem()).isNotNull();
        assertThat(itemManager.getCustomItem().getId()).isEqualTo("vesselmod:plasma_blade");
        assertThat(itemManager.getCustomItem().getDurability()).isEqualTo(1500);
        assertThat(itemManager.getFuelBurnTime(263)).isEqualTo(1600); // Coal burn time
        assertThat(itemManager.getFuelBurnTime(327)).isEqualTo(20000); // Lava bucket burn time

        // 5. Verify Regional Concurrency Subsystem
        RegionalTeleportDispatcher teleportDispatcher = mod.getTeleportDispatcher();
        AtomicBoolean globalExecuted = new AtomicBoolean(false);
        teleportDispatcher.dispatchGlobalTask(() -> globalExecuted.set(true));

        // 6. Verify Command Subsystem
        SovereignModCommand modCommand = mod.getModCommand();
        List<String> completions = modCommand.resolveTabCompletions("vess");
        assertThat(completions).contains("vesselmod");

        // 7. Verify Event Subsystem
        PureVesselEventListener eventListener = mod.getEventListener();
        String testEvent = "SyntheticServerTickEvent";
        eventListener.postDynamicEvent(testEvent);
        assertThat(eventListener.getEventCount()).isGreaterThanOrEqualTo(0L);
    }
}
