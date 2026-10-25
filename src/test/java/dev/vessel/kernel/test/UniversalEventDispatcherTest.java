package dev.vessel.kernel.test;

import dev.vessel.kernel.event.EventExecutionPhase;
import dev.vessel.kernel.event.UniversalEventDispatcher;
import dev.vessel.kernel.event.VesselCancellable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit verification suite for UniversalEventDispatcher and ArrayBackedEventInvoker.
 *
 * @version 1.0.0
 */
public class UniversalEventDispatcherTest {

    static final class MockBlockBreakEvent implements VesselCancellable {
        private final String blockType;
        private boolean cancelled;

        MockBlockBreakEvent(String blockType) {
            this.blockType = blockType;
        }

        public String getBlockType() {
            return blockType;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancel) {
            this.cancelled = cancel;
        }
    }

    @Test
    @DisplayName("UniversalEventDispatcher executes listeners in strict sequential phase order")
    void testPhaseOrdering() {
        UniversalEventDispatcher dispatcher = new UniversalEventDispatcher();
        List<String> executionOrder = new ArrayList<>();

        // Register out-of-order
        dispatcher.register(MockBlockBreakEvent.class, EventExecutionPhase.LATEST, e -> executionOrder.add("LATEST"));
        dispatcher.register(MockBlockBreakEvent.class, EventExecutionPhase.NORMAL, e -> executionOrder.add("NORMAL"));
        dispatcher.register(MockBlockBreakEvent.class, EventExecutionPhase.EARLIEST, e -> executionOrder.add("EARLIEST"));
        dispatcher.register(MockBlockBreakEvent.class, EventExecutionPhase.MONITOR, e -> executionOrder.add("MONITOR"));
        dispatcher.register(MockBlockBreakEvent.class, EventExecutionPhase.EARLY, e -> executionOrder.add("EARLY"));
        dispatcher.register(MockBlockBreakEvent.class, EventExecutionPhase.LATE, e -> executionOrder.add("LATE"));

        dispatcher.dispatch(new MockBlockBreakEvent("diamond_ore"));

        assertThat(executionOrder).containsExactly(
                "EARLIEST", "EARLY", "NORMAL", "LATE", "LATEST", "MONITOR"
        );
    }

    @Test
    @DisplayName("UniversalEventDispatcher skips non-monitor listeners when event is cancelled")
    void testCancellationShortCircuit() {
        UniversalEventDispatcher dispatcher = new UniversalEventDispatcher();
        List<String> executionOrder = new ArrayList<>();

        dispatcher.register(MockBlockBreakEvent.class, EventExecutionPhase.EARLY, e -> executionOrder.add("EARLY"));
        dispatcher.register(MockBlockBreakEvent.class, EventExecutionPhase.NORMAL, e -> {
            executionOrder.add("NORMAL_CANCEL");
            e.setCancelled(true);
        });
        dispatcher.register(MockBlockBreakEvent.class, EventExecutionPhase.LATE, e -> executionOrder.add("LATE_SKIPPED"));
        dispatcher.register(MockBlockBreakEvent.class, EventExecutionPhase.LATEST, e -> executionOrder.add("LATEST_SKIPPED"));
        dispatcher.register(MockBlockBreakEvent.class, EventExecutionPhase.MONITOR, e -> executionOrder.add("MONITOR_CALLED"));

        MockBlockBreakEvent event = new MockBlockBreakEvent("ancient_debris");
        dispatcher.dispatch(event);

        assertThat(event.isCancelled()).isTrue();
        assertThat(executionOrder).containsExactly(
                "EARLY", "NORMAL_CANCEL", "MONITOR_CALLED"
        );
    }

    @Test
    @DisplayName("EventExecutionPhase maps Bukkit and Forge priority names correctly")
    void testPriorityMapping() {
        assertThat(EventExecutionPhase.fromBukkit("LOWEST")).isEqualTo(EventExecutionPhase.EARLIEST);
        assertThat(EventExecutionPhase.fromBukkit("NORMAL")).isEqualTo(EventExecutionPhase.NORMAL);
        assertThat(EventExecutionPhase.fromBukkit("HIGHEST")).isEqualTo(EventExecutionPhase.LATEST);
        assertThat(EventExecutionPhase.fromBukkit("MONITOR")).isEqualTo(EventExecutionPhase.MONITOR);

        assertThat(EventExecutionPhase.fromForge("HIGHEST")).isEqualTo(EventExecutionPhase.EARLIEST);
        assertThat(EventExecutionPhase.fromForge("NORMAL")).isEqualTo(EventExecutionPhase.NORMAL);
        assertThat(EventExecutionPhase.fromForge("LOWEST")).isEqualTo(EventExecutionPhase.LATEST);
    }
}
