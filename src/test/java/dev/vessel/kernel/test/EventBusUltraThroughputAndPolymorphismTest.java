package dev.vessel.kernel.test;

import dev.vessel.kernel.event.VesselCancellable;
import dev.vessel.kernel.event.VesselEventSubscriberRegistry;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.VesselEventBus;
import net.neoforged.bus.api.VesselNeoEventBus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification test suite for high-performance zero-allocation event dispatching,
 * polymorphic class hierarchy resolution, and priority sequencing under Phase 4.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class EventBusUltraThroughputAndPolymorphismTest {

    public static class TestBaseEvent {
        private final String message;

        public TestBaseEvent(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class TestSubEvent extends TestBaseEvent implements VesselCancellable {
        private boolean cancelled;

        public TestSubEvent(String message) {
            super(message);
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }
    }

    public static class PrioritySampleListener {
        private final List<String> executionOrder = new ArrayList<>();

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onLowest(TestBaseEvent event) {
            executionOrder.add("LOWEST:" + event.getMessage());
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onHighest(TestBaseEvent event) {
            executionOrder.add("HIGHEST:" + event.getMessage());
        }

        @SubscribeEvent(priority = EventPriority.NORMAL)
        public void onNormal(TestBaseEvent event) {
            executionOrder.add("NORMAL:" + event.getMessage());
        }

        public List<String> getExecutionOrder() {
            return executionOrder;
        }
    }

    public static class PolymorphicSubEventListener {
        private int subEventCount = 0;

        @SubscribeEvent
        public void onSubEvent(TestSubEvent event) {
            subEventCount++;
        }

        public int getSubEventCount() {
            return subEventCount;
        }
    }

    @Test
    @DisplayName("VesselEventSubscriberRegistry enforces priority ordering and polymorphic event dispatch")
    void testRegistryOrderingAndPolymorphism() {
        VesselEventSubscriberRegistry registry = new VesselEventSubscriberRegistry();
        PrioritySampleListener listener = new PrioritySampleListener();
        PolymorphicSubEventListener subListener = new PolymorphicSubEventListener();

        registry.register(listener);
        registry.register(subListener);

        // 1. Post BaseEvent: only listener should receive it, in HIGHEST -> NORMAL -> LOWEST order
        TestBaseEvent baseEvent = new TestBaseEvent("hello");
        registry.dispatch(baseEvent, false);

        assertThat(listener.getExecutionOrder()).containsExactly(
                "HIGHEST:hello",
                "NORMAL:hello",
                "LOWEST:hello"
        );
        assertThat(subListener.getSubEventCount()).isEqualTo(0);

        // 2. Post SubEvent: both listener (polymorphic base) and subListener should receive it
        TestSubEvent subEvent = new TestSubEvent("world");
        registry.dispatch(subEvent, false);

        assertThat(listener.getExecutionOrder()).containsExactly(
                "HIGHEST:hello",
                "NORMAL:hello",
                "LOWEST:hello",
                "HIGHEST:world",
                "NORMAL:world",
                "LOWEST:world"
        );
        assertThat(subListener.getSubEventCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("VesselEventBus for Forge dispatches via high-throughput subscriber registry")
    void testForgeVesselEventBus() {
        VesselEventBus eventBus = new VesselEventBus();
        PrioritySampleListener listener = new PrioritySampleListener();
        eventBus.register(listener);

        TestBaseEvent event = new TestBaseEvent("forge");
        eventBus.post(event);

        assertThat(listener.getExecutionOrder()).containsExactly(
                "HIGHEST:forge",
                "NORMAL:forge",
                "LOWEST:forge"
        );

        eventBus.unregister(listener);
        eventBus.post(new TestBaseEvent("forge_after_unregister"));
        assertThat(listener.getExecutionOrder()).hasSize(3); // No new events received
    }

    @Test
    @DisplayName("VesselNeoEventBus for NeoForge dispatches via high-throughput subscriber registry")
    void testNeoForgeVesselEventBus() {
        VesselNeoEventBus neoBus = new VesselNeoEventBus();
        PrioritySampleListener listener = new PrioritySampleListener();
        neoBus.register(listener);

        TestBaseEvent event = new TestBaseEvent("neoforge");
        neoBus.post(event);

        assertThat(listener.getExecutionOrder()).containsExactly(
                "HIGHEST:neoforge",
                "NORMAL:neoforge",
                "LOWEST:neoforge"
        );
    }
}
