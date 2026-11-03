package dev.vessel.bootstrap.wave1;

import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;

/**
 * Sample Forge {@code @Mod} test fixture verifying Wave 1 typed lifecycle event dispatch
 * ({@link FMLConstructModEvent}, {@link FMLCommonSetupEvent},
 * {@link FMLDedicatedServerSetupEvent}, {@link FMLLoadCompleteEvent}), deferred work queue
 * execution via {@link FMLCommonSetupEvent#enqueueWork(Runnable)}, and
 * {@link SubscribeEvent} priority ordering ({@link EventPriority#HIGH} before
 * {@link EventPriority#LOW}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@Mod("wave1_forge_test")
public final class SampleForgeWaveOneModFixture {

    private final List<String> lifecycleLog = new ArrayList<>();

    public SampleForgeWaveOneModFixture() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onConstructLow(FMLConstructModEvent event) {
        lifecycleLog.add("FORGE_CONSTRUCT_LOW:" + event.getModId());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onConstructHigh(FMLConstructModEvent event) {
        lifecycleLog.add("FORGE_CONSTRUCT_HIGH:" + event.getModId());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onCommonSetupLow(FMLCommonSetupEvent event) {
        lifecycleLog.add("FORGE_COMMON_SETUP_LOW:" + event.getModId());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onCommonSetupHigh(FMLCommonSetupEvent event) {
        lifecycleLog.add("FORGE_COMMON_SETUP_HIGH:" + event.getModId());
        event.enqueueWork(this::recordEnqueuedWork);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onDedicatedServerSetupLow(FMLDedicatedServerSetupEvent event) {
        lifecycleLog.add("FORGE_DEDICATED_SERVER_LOW:" + event.getModId());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onDedicatedServerSetupHigh(FMLDedicatedServerSetupEvent event) {
        lifecycleLog.add("FORGE_DEDICATED_SERVER_HIGH:" + event.getModId());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onLoadCompleteLow(FMLLoadCompleteEvent event) {
        lifecycleLog.add("FORGE_LOAD_COMPLETE_LOW:" + event.getModId());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onLoadCompleteHigh(FMLLoadCompleteEvent event) {
        lifecycleLog.add("FORGE_LOAD_COMPLETE_HIGH:" + event.getModId());
    }

    public void recordEnqueuedWork() {
        lifecycleLog.add("FORGE_ENQUEUED_WORK");
    }

    public List<String> getLifecycleLog() {
        return lifecycleLog;
    }
}
