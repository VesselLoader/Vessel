package dev.vessel.bootstrap.wave1;

import java.util.ArrayList;
import java.util.List;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;

/**
 * Sample NeoForge {@code @Mod} test fixture verifying Wave 1 typed lifecycle event dispatch
 * ({@link FMLConstructModEvent}, {@link FMLCommonSetupEvent},
 * {@link FMLDedicatedServerSetupEvent}, {@link FMLLoadCompleteEvent}), constructor injection of
 * {@link IEventBus}, deferred work queue execution via {@link FMLCommonSetupEvent#enqueueWork(Runnable)},
 * and {@link SubscribeEvent} priority ordering.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@Mod("wave1_neoforge_test")
public final class SampleNeoForgeWaveOneModFixture {

    private final List<String> lifecycleLog = new ArrayList<>();

    public SampleNeoForgeWaveOneModFixture(IEventBus modBus) {
        if (modBus != null && !modBus.getRegisteredListeners().contains(this)) {
            modBus.register(this);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onConstructLow(FMLConstructModEvent event) {
        lifecycleLog.add("NEOFORGE_CONSTRUCT_LOW:" + event.getModId());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onConstructHigh(FMLConstructModEvent event) {
        lifecycleLog.add("NEOFORGE_CONSTRUCT_HIGH:" + event.getModId());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onCommonSetupLow(FMLCommonSetupEvent event) {
        lifecycleLog.add("NEOFORGE_COMMON_SETUP_LOW:" + event.getModId());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onCommonSetupHigh(FMLCommonSetupEvent event) {
        lifecycleLog.add("NEOFORGE_COMMON_SETUP_HIGH:" + event.getModId());
        event.enqueueWork(this::recordEnqueuedWork);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onDedicatedServerSetupLow(FMLDedicatedServerSetupEvent event) {
        lifecycleLog.add("NEOFORGE_DEDICATED_SERVER_LOW:" + event.getModId());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onDedicatedServerSetupHigh(FMLDedicatedServerSetupEvent event) {
        lifecycleLog.add("NEOFORGE_DEDICATED_SERVER_HIGH:" + event.getModId());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onLoadCompleteLow(FMLLoadCompleteEvent event) {
        lifecycleLog.add("NEOFORGE_LOAD_COMPLETE_LOW:" + event.getModId());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onLoadCompleteHigh(FMLLoadCompleteEvent event) {
        lifecycleLog.add("NEOFORGE_LOAD_COMPLETE_HIGH:" + event.getModId());
    }

    public void recordEnqueuedWork() {
        lifecycleLog.add("NEOFORGE_ENQUEUED_WORK");
    }

    public List<String> getLifecycleLog() {
        return lifecycleLog;
    }
}
