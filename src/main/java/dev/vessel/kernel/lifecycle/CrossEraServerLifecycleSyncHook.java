package dev.vessel.kernel.lifecycle;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraftforge.common.MinecraftForge;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Synchronizes server lifecycle transitions across all supported mod and plugin ecosystems,
 * firing corresponding Forge, NeoForge, and Fabric lifecycle events when the server transitions
 * to {@code SERVER_STARTING} and {@code SERVER_STARTED}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public final class CrossEraServerLifecycleSyncHook {

    private volatile boolean startingFired;
    private volatile boolean startedFired;

    /**
     * Dispatches the {@code SERVER_STARTING} lifecycle milestone across Forge, NeoForge, and Fabric.
     *
     * @param server the active running server instance
     */
    public synchronized void onServerStarting(Object server) {
        if (startingFired) {
            return;
        }
        startingFired = true;
        VesselServerLifecycleStateTracker.markServerStarting(server);

        try {
            MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.server.ServerStartingEvent(server));
        } catch (Throwable t) {
            System.err.println("[VESSEL] Notice: Forge ServerStartingEvent error: " + t.getMessage());
        }

        try {
            NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.server.ServerStartingEvent(server));
        } catch (Throwable t) {
            System.err.println("[VESSEL] Notice: NeoForge ServerStartingEvent error: " + t.getMessage());
        }

        try {
            ServerLifecycleEvents.SERVER_STARTING.invoker().onServerLifecycle(server);
        } catch (Throwable t) {
            System.err.println("[VESSEL] Notice: Fabric SERVER_STARTING callback error: " + t.getMessage());
        }
    }

    /**
     * Dispatches the {@code SERVER_STARTED} lifecycle milestone across Forge, NeoForge, and Fabric.
     *
     * @param server the active running server instance
     */
    public synchronized void onServerStarted(Object server) {
        if (startedFired) {
            return;
        }
        startedFired = true;
        VesselServerLifecycleStateTracker.markServerReady(server);

        try {
            MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.server.ServerStartedEvent(server));
        } catch (Throwable t) {
            System.err.println("[VESSEL] Notice: Forge ServerStartedEvent error: " + t.getMessage());
        }

        try {
            NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.server.ServerStartedEvent(server));
        } catch (Throwable t) {
            System.err.println("[VESSEL] Notice: NeoForge ServerStartedEvent error: " + t.getMessage());
        }

        try {
            ServerLifecycleEvents.SERVER_STARTED.invoker().onServerLifecycle(server);
        } catch (Throwable t) {
            System.err.println("[VESSEL] Notice: Fabric SERVER_STARTED callback error: " + t.getMessage());
        }
    }
}
