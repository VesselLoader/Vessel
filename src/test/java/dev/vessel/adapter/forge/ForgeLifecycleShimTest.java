package dev.vessel.adapter.forge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vessel.adapter.neoforge.NeoForgeMetadataAdapter;
import dev.vessel.kernel.event.EventExecutionPhase;
import dev.vessel.kernel.identity.SourceLoaderKind;
import dev.vessel.kernel.identity.VesselModuleIdentifier;
import dev.vessel.kernel.identity.dependency.DependencyConstraintKind;
import dev.vessel.kernel.identity.dependency.ModuleDependencyRule;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Sub-Stage 6.6: {@link NeoForgeMetadataAdapter},
 * {@link SovereignEventBusShim}, {@link DeferredRegisterBridge},
 * {@link ForgeLifecycleAdapter}, {@link UniversalInventoryBridge}, and
 * {@link CustomEffectRegistryBridge}.
 *
 * @version 1.0.0
 */
class ForgeLifecycleShimTest {

    @Test
    void testNeoForgeMetadataAdapterParsesIdentifierAndDependencies() {
        String toml = """
                modLoader="javafml"
                loaderVersion="[4,)"
                license="MIT"

                [[mods]]
                modId="mekanism"
                version="10.7.8"
                displayName="Mekanism"

                [[dependencies.mekanism]]
                modId="neoforge"
                type="required"
                versionRange="[21.1.0,)"
                ordering="NONE"

                [[dependencies.mekanism]]
                modId="curios"
                type="optional"
                versionRange="[9.0.0,)"

                [[dependencies.mekanism]]
                modId="optifine"
                type="incompatible"
                versionRange="*"
                """;

        NeoForgeMetadataAdapter adapter = new NeoForgeMetadataAdapter();
        VesselModuleIdentifier id = adapter.parseNeoForgeToml(toml);
        assertEquals(SourceLoaderKind.NEOFORGE, id.loader());
        assertEquals("mekanism", id.namespace());
        assertEquals("10.7.8", id.version());

        List<ModuleDependencyRule> deps = adapter.extractNeoForgeDependencies(toml);
        assertEquals(3, deps.size());
        assertEquals("neoforge", deps.get(0).targetNamespace());
        assertEquals(DependencyConstraintKind.MANDATORY, deps.get(0).kind());
        assertEquals("curios", deps.get(1).targetNamespace());
        assertEquals(DependencyConstraintKind.OPTIONAL, deps.get(1).kind());
        assertEquals("optifine", deps.get(2).targetNamespace());
        assertEquals(DependencyConstraintKind.CONFLICTS, deps.get(2).kind());
    }

    @Test
    void testSovereignEventBusShimPriorityOrderingAndCancellationSemantics() {
        SovereignEventBusShim eventBus = new SovereignEventBusShim();
        List<String> callOrder = new ArrayList<>();

        eventBus.registerListener(
                "LivingHurtEvent",
                EventExecutionPhase.LATE,
                false,
                ev -> callOrder.add("LATE_SKIPPED_WHEN_CANCELLED"));
        eventBus.registerListener(
                "LivingHurtEvent",
                EventExecutionPhase.LATE,
                true,
                ev -> callOrder.add("LATE_RECEIVE_CANCELLED"));
        eventBus.registerListener(
                "LivingHurtEvent",
                EventExecutionPhase.EARLIEST,
                false,
                ev -> {
                    callOrder.add("EARLIEST_CANCELLER");
                    if (ev instanceof AtomicBoolean flag) {
                        flag.set(true);
                    }
                });

        AtomicBoolean cancellablePayload = new AtomicBoolean(false);
        boolean wasCancelled = eventBus.postEvent("LivingHurtEvent", cancellablePayload);
        assertTrue(wasCancelled);
        assertEquals(
                List.of("EARLIEST_CANCELLER", "LATE_RECEIVE_CANCELLED"),
                callOrder);
    }

    @Test
    void testDeferredRegisterBridgeRetroactiveReplayAndForgeLifecycleSetupQueue() {
        DeferredRegisterBridge deferred = new DeferredRegisterBridge();
        int blockId = deferred.registerBlockEntry("create", "mechanical_press", 4);
        int itemId = deferred.registerItemEntry(
                "create", "wrench", Map.of("minecraft:max_stack_size", 1));
        assertTrue(blockId >= 1000);
        assertTrue(itemId > blockId);

        List<String> replayedBlocks = new ArrayList<>();
        deferred.registerEntryAddedCallback("minecraft:block", replayedBlocks::add);
        assertEquals(List.of("create:mechanical_press"), replayedBlocks);

        deferred.registerBlockEntry("create", "cogwheel", 6);
        assertEquals(List.of("create:mechanical_press", "create:cogwheel"), replayedBlocks);

        ForgeLifecycleAdapter lifecycle = new ForgeLifecycleAdapter();
        List<String> setupTasks = new ArrayList<>();
        lifecycle.enqueueCommonSetupWork("create", () -> setupTasks.add("create:network_init"));
        lifecycle.enqueueCommonSetupWork("mekanism", () -> setupTasks.add("mekanism:transmitters"));
        assertEquals(2, lifecycle.flushCommonSetupQueue());
        assertEquals(List.of("create:network_init", "mekanism:transmitters"), setupTasks);
        assertTrue(lifecycle.getReplayableRegistrationEvents().contains("RegisterEvent"));
    }

    @Test
    void testUniversalInventoryBridgeAndCustomEffectRegistryBridge() {
        UniversalInventoryBridge inventoryBridge = new UniversalInventoryBridge();
        assertTrue(inventoryBridge.setItemInModdedSlot("ae2:drive_01", 3, "ae2:item_storage_cell_64k", 1));
        assertEquals(4, inventoryBridge.getBridgedContainerSlotCount("ae2:drive_01"));
        assertEquals(
                "ae2:item_storage_cell_64k*1",
                inventoryBridge.getItemInModdedSlot("ae2:drive_01", 3));
        assertEquals("minecraft:air*0", inventoryBridge.getItemInModdedSlot("ae2:drive_01", 0));

        CustomEffectRegistryBridge effectBridge = new CustomEffectRegistryBridge();
        effectBridge.registerModdedEnchantment("apotheosis:capturing", 5);
        effectBridge.registerModdedPotionEffect("ars_nouveau:mana_regen", true);

        assertTrue(effectBridge.isRegisteredBukkitEnchantment("apotheosis:capturing"));
        assertFalse(effectBridge.isRegisteredBukkitEnchantment("minecraft:unknown"));
        assertTrue(effectBridge.isRegisteredBukkitPotionEffect("ars_nouveau:mana_regen"));
        assertFalse(effectBridge.isRegisteredBukkitPotionEffect("minecraft:unknown"));
    }
}
