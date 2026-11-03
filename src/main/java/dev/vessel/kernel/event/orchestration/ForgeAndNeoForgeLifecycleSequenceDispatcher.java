package dev.vessel.kernel.event.orchestration;

import java.util.Objects;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;

/**
 * Orchestrates the canonical 4-stage mod initialization lifecycle event sequence across
 * Minecraft Forge ({@link net.minecraftforge.eventbus.api.IEventBus}) and NeoForge
 * ({@link net.neoforged.bus.api.IEventBus}) mod event buses.
 *
 * <p>Fires {@code FMLConstructModEvent}, {@code FMLCommonSetupEvent} (draining all deferred
 * main-thread work queued via {@code enqueueWork}), {@code FMLDedicatedServerSetupEvent},
 * and {@code FMLLoadCompleteEvent} in deterministic order.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ForgeAndNeoForgeLifecycleSequenceDispatcher {

    /**
     * Constructs a new Forge and NeoForge lifecycle sequence dispatcher.
     */
    public ForgeAndNeoForgeLifecycleSequenceDispatcher() {
    }

    /**
     * Dispatches the 4-stage FML mod initialization lifecycle sequence onto a Minecraft Forge
     * {@link net.minecraftforge.eventbus.api.IEventBus}.
     *
     * @param modId    mod identifier (normalized to {@code "minecraft"} if {@code null} or blank)
     * @param forgeBus non-null Forge mod event bus
     * @return the number of lifecycle stages dispatched ({@code 4})
     */
    public int dispatchForgeModLifecycleSequence(
            String modId,
            net.minecraftforge.eventbus.api.IEventBus forgeBus) {
        Objects.requireNonNull(forgeBus, "forgeBus must not be null");
        String safeModId = (modId == null || modId.isBlank()) ? "minecraft" : modId;

        forgeBus.post(new FMLConstructModEvent(safeModId));

        FMLCommonSetupEvent commonSetup = new FMLCommonSetupEvent(safeModId);
        forgeBus.post(commonSetup);
        commonSetup.drainEnqueuedWork();
        forgeBus.getUnderlyingShim().postEvent("FMLCommonSetupEvent", commonSetup);
        commonSetup.drainEnqueuedWork();

        forgeBus.post(new FMLDedicatedServerSetupEvent(safeModId));
        forgeBus.post(new FMLLoadCompleteEvent(safeModId));
        return 4;
    }

    /**
     * Dispatches the 4-stage FML mod initialization lifecycle sequence onto a NeoForge
     * {@link net.neoforged.bus.api.IEventBus}.
     *
     * @param modId       mod identifier (normalized to {@code "minecraft"} if {@code null} or blank)
     * @param neoForgeBus non-null NeoForge mod event bus
     * @return the number of lifecycle stages dispatched ({@code 4})
     */
    public int dispatchNeoForgeModLifecycleSequence(
            String modId,
            net.neoforged.bus.api.IEventBus neoForgeBus) {
        Objects.requireNonNull(neoForgeBus, "neoForgeBus must not be null");
        String safeModId = (modId == null || modId.isBlank()) ? "minecraft" : modId;

        neoForgeBus.post(new net.neoforged.fml.event.lifecycle.FMLConstructModEvent(safeModId));

        net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent commonSetup =
                new net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent(safeModId);
        neoForgeBus.post(commonSetup);
        commonSetup.drainEnqueuedWork();
        neoForgeBus.getUnderlyingShim().postEvent("FMLCommonSetupEvent", commonSetup);
        commonSetup.drainEnqueuedWork();

        neoForgeBus.post(new net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent(safeModId));
        neoForgeBus.post(new net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent(safeModId));
        return 4;
    }
}
