package dev.vessel.command;

import dev.vessel.kernel.lifecycle.CrossEraServerLifecycleSyncHook;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test suite verifying Forge, NeoForge, and Fabric deferred command registration and
 * multi-ecosystem lifecycle synchronization hooks.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class ForgeAndNeoForgeDeferredCommandAndLifecycleTest {

    @Test
    void testForgeAndNeoForgeRegisterCommandsBridgeDispatchesToEventBuses() {
        AtomicBoolean forgeReceived = new AtomicBoolean(false);
        AtomicBoolean neoReceived = new AtomicBoolean(false);

        MinecraftForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event -> {
            forgeReceived.set(true);
            assertNotNull(event.getDispatcher());
        });

        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.RegisterCommandsEvent.class, event -> {
            neoReceived.set(true);
            assertNotNull(event.getDispatcher());
        });

        ForgeDeferredCommandRegistrationBridge bridge = new ForgeDeferredCommandRegistrationBridge();
        com.mojang.brigadier.CommandDispatcher<Object> mockDispatcher =
                new com.mojang.brigadier.CommandDispatcher<>();

        boolean ok = bridge.dispatchRegisterCommandsEvent(
                mockDispatcher,
                getClass().getClassLoader());

        assertTrue(ok);
        assertTrue(bridge.isDispatched());
        assertTrue(forgeReceived.get());
        assertTrue(neoReceived.get());
    }

    @Test
    void testCrossEraServerLifecycleSyncHookFiresAcrossEcosystems() {
        AtomicBoolean forgeStarting = new AtomicBoolean(false);
        AtomicBoolean neoStarting = new AtomicBoolean(false);
        AtomicBoolean fabricStarting = new AtomicBoolean(false);
        AtomicBoolean forgeStarted = new AtomicBoolean(false);
        AtomicBoolean neoStarted = new AtomicBoolean(false);
        AtomicBoolean fabricStarted = new AtomicBoolean(false);

        MinecraftForge.EVENT_BUS.addListener(ServerStartingEvent.class, e -> forgeStarting.set(true));
        MinecraftForge.EVENT_BUS.addListener(ServerStartedEvent.class, e -> forgeStarted.set(true));

        NeoForge.EVENT_BUS.addListener(
                net.neoforged.neoforge.event.server.ServerStartingEvent.class,
                e -> neoStarting.set(true));
        NeoForge.EVENT_BUS.addListener(
                net.neoforged.neoforge.event.server.ServerStartedEvent.class,
                e -> neoStarted.set(true));

        ServerLifecycleEvents.SERVER_STARTING.register(server -> fabricStarting.set(true));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> fabricStarted.set(true));

        CrossEraServerLifecycleSyncHook syncHook = new CrossEraServerLifecycleSyncHook();
        Object mockServer = "MockServerInstance";

        syncHook.onServerStarting(mockServer);
        assertTrue(forgeStarting.get());
        assertTrue(neoStarting.get());
        assertTrue(fabricStarting.get());

        syncHook.onServerStarted(mockServer);
        assertTrue(forgeStarted.get());
        assertTrue(neoStarted.get());
        assertTrue(fabricStarted.get());
    }
}
