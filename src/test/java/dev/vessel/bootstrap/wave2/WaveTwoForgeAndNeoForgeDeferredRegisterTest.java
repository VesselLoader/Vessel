package dev.vessel.bootstrap.wave2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Wave 2 verifying MinecraftForge {@link DeferredRegister} /
 * {@link RegistryObject} and NeoForge {@link net.neoforged.neoforge.registries.DeferredRegister} /
 * {@link DeferredHolder} event-bus binding and entry resolution.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
final class WaveTwoForgeAndNeoForgeDeferredRegisterTest {

    @Test
    void testForgeDeferredRegisterResolvesRegistryObjectOnModBusLifecycleEvent() {
        DeferredRegister<String> forgeItems =
                DeferredRegister.create("minecraft:item", "create");
        RegistryObject<String> copperGear =
                forgeItems.register("copper_gear", new SampleRegistryValueSupplier("copper_gear"));

        assertEquals("create:copper_gear", copperGear.getId());
        assertFalse(copperGear.isPresent());
        assertEquals("fallback", copperGear.orElse("fallback"));

        net.minecraftforge.eventbus.api.IEventBus forgeBus =
                new net.minecraftforge.eventbus.api.VesselEventBus();
        forgeItems.register(forgeBus);

        forgeBus.post(new FMLConstructModEvent("create"));

        assertTrue(copperGear.isPresent());
        assertEquals("copper_gear", copperGear.get());
        assertEquals(1, forgeItems.getEntries().size());
    }

    @Test
    void testNeoForgeDeferredRegisterResolvesDeferredHolderOnModBusLifecycleEvent() {
        net.neoforged.neoforge.registries.DeferredRegister<String> neoForgeItems =
                net.neoforged.neoforge.registries.DeferredRegister.create(
                        "minecraft:item",
                        "mekanism");
        DeferredHolder<String, String> atomicAlloy =
                neoForgeItems.register(
                        "atomic_alloy",
                        new SampleRegistryValueSupplier("atomic_alloy"));

        assertEquals("mekanism:atomic_alloy", atomicAlloy.getId());
        assertFalse(atomicAlloy.isBound());
        assertEquals("fallback", atomicAlloy.orElse("fallback"));

        net.neoforged.bus.api.IEventBus neoForgeBus = new net.neoforged.bus.api.VesselNeoEventBus();
        neoForgeItems.register(neoForgeBus);

        neoForgeBus.post(new net.neoforged.fml.event.lifecycle.FMLConstructModEvent("mekanism"));

        assertTrue(atomicAlloy.isBound());
        assertEquals("atomic_alloy", atomicAlloy.value());
        assertEquals("atomic_alloy", atomicAlloy.get());
        assertEquals(1, neoForgeItems.getEntries().size());
    }
}
