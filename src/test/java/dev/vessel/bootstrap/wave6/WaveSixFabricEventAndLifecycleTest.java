package dev.vessel.bootstrap.wave6;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Wave 6 verifying Fabric {@link Event}, {@link EventFactory},
 * {@link ServerLifecycleEvents}, and {@link ServerTickEvents}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveSixFabricEventAndLifecycleTest {

    private final List<String> lifecycleTrace = new ArrayList<>();

    public void onServerStarting(Object serverInstance) {
        lifecycleTrace.add("STARTING:" + serverInstance);
    }

    public void onServerStarted(Object serverInstance) {
        lifecycleTrace.add("STARTED:" + serverInstance);
    }

    public void onEndServerTick(Object serverInstance) {
        lifecycleTrace.add("END_TICK:" + serverInstance);
    }

    private static ServerLifecycleCallback createCustomComposite(
            List<ServerLifecycleCallback> listeners) {
        return server -> {
            for (ServerLifecycleCallback listener : listeners) {
                listener.onServerLifecycle(server);
            }
        };
    }

    @Test
    void testFabricEventFactoryServerLifecycleEventsAndServerTickEvents() {
        lifecycleTrace.clear();
        ServerLifecycleEvents.SERVER_STARTING.clearListeners();
        ServerLifecycleEvents.SERVER_STARTED.clearListeners();
        ServerTickEvents.END_SERVER_TICK.clearListeners();

        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerTickEvents.END_SERVER_TICK.register(this::onEndServerTick);

        assertEquals(1, ServerLifecycleEvents.SERVER_STARTING.getRegisteredListeners().size());
        assertEquals(1, ServerLifecycleEvents.SERVER_STARTED.getRegisteredListeners().size());
        assertEquals(1, ServerTickEvents.END_SERVER_TICK.getRegisteredListeners().size());

        ServerLifecycleEvents.SERVER_STARTING.invoker().onServerLifecycle("DedicatedServer");
        ServerLifecycleEvents.SERVER_STARTED.invoker().onServerLifecycle("DedicatedServer");
        ServerTickEvents.END_SERVER_TICK.invoker().onServerLifecycle("DedicatedServer");

        assertEquals(
                List.of(
                        "STARTING:DedicatedServer",
                        "STARTED:DedicatedServer",
                        "END_TICK:DedicatedServer"),
                lifecycleTrace);

        Event<ServerLifecycleCallback> customEvent = EventFactory.createArrayBacked(
                ServerLifecycleCallback.class,
                WaveSixFabricEventAndLifecycleTest::createCustomComposite);
        assertNotNull(customEvent);
        customEvent.register(this::onEndServerTick);
        assertEquals(1, customEvent.getRegisteredListeners().size());
        customEvent.invoker().onServerLifecycle("CustomServer");
        assertEquals(4, lifecycleTrace.size());
        assertEquals("END_TICK:CustomServer", lifecycleTrace.get(3));

        ServerLifecycleEvents.SERVER_STARTING.clearListeners();
        ServerLifecycleEvents.SERVER_STARTED.clearListeners();
        ServerTickEvents.END_SERVER_TICK.clearListeners();
    }
}
