package dev.vessel.bootstrap.wave1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import dev.vessel.kernel.event.EventExecutionPhase;
import dev.vessel.kernel.event.orchestration.CrossLoaderCancellationStateBridge;
import dev.vessel.kernel.event.orchestration.MultiLoaderPriorityPhaseMapper;
import dev.vessel.kernel.identity.SourceLoaderKind;
import java.util.List;
import org.bukkit.event.EventPriority;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Wave 1 ({@code ADR-0002} &amp; {@code ADR-0003}) verifying Bukkit
 * {@code @EventHandler} priority ordering, {@code ignoreCancelled} filtering,
 * {@code MONITOR} phase read-only cancellation enforcement, {@link ServerLoadEvent} dispatch,
 * {@link MultiLoaderPriorityPhaseMapper}, and {@link CrossLoaderCancellationStateBridge}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
final class WaveOneBukkitEventHandlerPriorityAndCancelTest {

    @Test
    void testBukkitEventHandlerPriorityOrderingIgnoreCancelledAndMonitorReadOnlyLock() {
        PluginManager pluginManager = new org.bukkit.plugin.VesselPluginManager();
        JavaPlugin plugin = new JavaPlugin();
        plugin.init(
                new PluginDescriptionFile(
                        "WaveOneBukkitTestPlugin",
                        "1.0.0",
                        JavaPlugin.class.getName(),
                        "1.21"),
                plugin.getServer());
        pluginManager.enablePlugin(plugin);

        SampleBukkitWaveOneListenerFixture listener = new SampleBukkitWaveOneListenerFixture();
        pluginManager.registerEvents(listener, plugin);

        SampleBukkitWaveOneCancelableEvent cancelableEvent =
                new SampleBukkitWaveOneCancelableEvent();
        pluginManager.callEvent(cancelableEvent);

        List<String> order = cancelableEvent.getExecutionOrder();
        assertEquals(
                List.of("LOWEST", "NORMAL", "HIGH_RECEIVED", "MONITOR"),
                order,
                "Handlers must execute in LOWEST -> NORMAL -> HIGH -> MONITOR order and skip HIGH_IGNORED");
        assertFalse(order.contains("HIGH_IGNORED"));
        assertTrue(
                cancelableEvent.isCancelled(),
                "ReflectiveBukkitEventConsumer must restore cancelled=true after MONITOR attempts to uncancel");

        pluginManager.callEvent(new ServerLoadEvent(ServerLoadEvent.STARTUP_TYPE));
        pluginManager.callEvent(new ServerLoadEvent(ServerLoadEvent.RELOAD_TYPE));
        assertEquals(
                List.of(ServerLoadEvent.STARTUP_TYPE, ServerLoadEvent.RELOAD_TYPE),
                listener.getObservedServerLoadTypes());
    }

    @Test
    void testMultiLoaderPriorityPhaseMapperAndCrossLoaderCancellationStateBridge() {
        MultiLoaderPriorityPhaseMapper mapper = new MultiLoaderPriorityPhaseMapper();

        assertEquals(
                EventExecutionPhase.EARLIEST,
                mapper.mapForgePriority(net.minecraftforge.eventbus.api.EventPriority.HIGHEST));
        assertEquals(
                EventExecutionPhase.LATEST,
                mapper.mapForgePriority(net.minecraftforge.eventbus.api.EventPriority.LOWEST));
        assertEquals(
                EventExecutionPhase.EARLIEST,
                mapper.mapNeoForgePriority(net.neoforged.bus.api.EventPriority.HIGHEST));
        assertEquals(
                EventExecutionPhase.EARLIEST,
                mapper.mapBukkitPriority(EventPriority.LOWEST));
        assertEquals(
                EventExecutionPhase.LATEST,
                mapper.mapBukkitPriority(EventPriority.HIGHEST));
        assertEquals(
                EventExecutionPhase.MONITOR,
                mapper.mapBukkitPriority(EventPriority.MONITOR));

        assertTrue(
                mapper.compareSubPhaseOrder(SourceLoaderKind.FABRIC, SourceLoaderKind.FORGE) < 0);
        assertTrue(
                mapper.compareSubPhaseOrder(SourceLoaderKind.NEOFORGE, SourceLoaderKind.PAPER) < 0);
        assertEquals(
                0,
                mapper.compareSubPhaseOrder(SourceLoaderKind.PAPER, SourceLoaderKind.FOLIA));

        CrossLoaderCancellationStateBridge cancellationBridge =
                new CrossLoaderCancellationStateBridge();
        SampleBukkitWaveOneCancelableEvent sourceEvent = new SampleBukkitWaveOneCancelableEvent();
        SampleBukkitWaveOneCancelableEvent targetEvent = new SampleBukkitWaveOneCancelableEvent();

        assertFalse(cancellationBridge.isEventCancelled(sourceEvent));
        cancellationBridge.setEventCancelled(sourceEvent, true, EventExecutionPhase.NORMAL);
        assertTrue(cancellationBridge.isEventCancelled(sourceEvent));

        cancellationBridge.synchronizeCancellation(
                sourceEvent,
                targetEvent,
                EventExecutionPhase.LATE);
        assertTrue(cancellationBridge.isEventCancelled(targetEvent));

        try {
            cancellationBridge.setEventCancelled(
                    targetEvent,
                    false,
                    EventExecutionPhase.MONITOR);
            fail("Expected IllegalStateException when mutating cancellation state in MONITOR phase");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("MONITOR"));
        }
        assertTrue(cancellationBridge.isEventCancelled(targetEvent));
    }
}
