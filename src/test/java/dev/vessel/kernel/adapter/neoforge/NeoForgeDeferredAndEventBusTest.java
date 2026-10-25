package dev.vessel.kernel.adapter.neoforge;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests verifying NeoForge DeferredRegister bridge mechanics with retroactive replay
 * and EventBus 8 priority-based dispatcher honoring cancellation semantics.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class NeoForgeDeferredAndEventBusTest {

    private NeoForgeDeferredRegisterBridge registerBridge;
    private NeoForgeEventBusDispatcher eventBusDispatcher;

    @BeforeEach
    public void setUp() {
        registerBridge = new NeoForgeDeferredRegisterBridge();
        eventBusDispatcher = new NeoForgeEventBusDispatcher();
    }

    @Test
    @DisplayName("NeoForge DeferredRegister - Queues entries and assigns incremental raw IDs")
    public void testDeferredRegistration() {
        NeoForgeRegistryHolder holder1 = registerBridge.register(
                "minecraft:block",
                "testmod",
                "ruby_block",
                () -> "RubyBlockInstance"
        );

        NeoForgeRegistryHolder holder2 = registerBridge.register(
                "minecraft:block",
                "testmod",
                "sapphire_block",
                () -> "SapphireBlockInstance"
        );

        assertThat(holder1.rawNumericId()).isEqualTo(20000);
        assertThat(holder2.rawNumericId()).isEqualTo(20001);
        assertThat(registerBridge.resolveInstance("testmod:ruby_block")).isEqualTo("RubyBlockInstance");
        assertThat(registerBridge.getEntries("minecraft:block")).hasSize(2);
    }

    @Test
    @DisplayName("NeoForge DeferredRegister - Retroactively replays entries to late-registering callbacks")
    public void testRetroactiveCallbackReplay() {
        registerBridge.register("minecraft:item", "mod_a", "sword", () -> "SwordItem");
        registerBridge.register("minecraft:item", "mod_a", "shield", () -> "ShieldItem");

        List<String> replayedKeys = new ArrayList<>();
        // Late-registering callback
        registerBridge.registerCallback("minecraft:item", holder -> replayedKeys.add(holder.namespacedKey()));

        // Should immediately receive existing 2 entries
        assertThat(replayedKeys).containsExactly("mod_a:sword", "mod_a:shield");

        // New entry registered after callback registration
        registerBridge.register("minecraft:item", "mod_a", "bow", () -> "BowItem");
        assertThat(replayedKeys).containsExactly("mod_a:sword", "mod_a:shield", "mod_a:bow");
    }

    @Test
    @DisplayName("NeoForge DeferredRegister - Rejects duplicate deferred key registrations with ERR-92")
    public void testDuplicateDeferredRegistrationRejection() {
        registerBridge.register("minecraft:block", "mod_a", "ore", () -> "Ore1");

        assertThatThrownBy(() -> registerBridge.register("minecraft:block", "mod_a", "ore", () -> "Ore2"))
                .isInstanceOf(NeoForgeDeferredRegisterException.class)
                .hasMessageContaining("[ERR-92]")
                .hasMessageContaining("Duplicate deferred registration");
    }

    @Test
    @DisplayName("NeoForge EventBus 8 - Invokes subscribers in strict priority order (HIGHEST -> LOWEST)")
    public void testPriorityOrderInvocation() {
        List<String> invocationLog = new ArrayList<>();

        eventBusDispatcher.addListener(
                TestSimpleBusEvent.class,
                NeoForgeEventPriority.LOW,
                false,
                ev -> invocationLog.add("LOW"),
                "low-sub"
        );

        eventBusDispatcher.addListener(
                TestSimpleBusEvent.class,
                NeoForgeEventPriority.HIGHEST,
                false,
                ev -> invocationLog.add("HIGHEST"),
                "highest-sub"
        );

        eventBusDispatcher.addListener(
                TestSimpleBusEvent.class,
                NeoForgeEventPriority.NORMAL,
                false,
                ev -> invocationLog.add("NORMAL"),
                "normal-sub"
        );

        eventBusDispatcher.addListener(
                TestSimpleBusEvent.class,
                NeoForgeEventPriority.HIGH,
                false,
                ev -> invocationLog.add("HIGH"),
                "high-sub"
        );

        eventBusDispatcher.addListener(
                TestSimpleBusEvent.class,
                NeoForgeEventPriority.LOWEST,
                false,
                ev -> invocationLog.add("LOWEST"),
                "lowest-sub"
        );

        boolean uncanceled = eventBusDispatcher.post(new TestSimpleBusEvent("payload-data"));

        assertThat(uncanceled).isTrue();
        assertThat(invocationLog).containsExactly("HIGHEST", "HIGH", "NORMAL", "LOW", "LOWEST");
    }

    @Test
    @DisplayName("NeoForge EventBus 8 - Respects cancellation and receiveCancelled flags")
    public void testEventCancellationSemantics() {
        AtomicBoolean normalInvoked = new AtomicBoolean(false);
        AtomicBoolean lowWithReceiveCancelledInvoked = new AtomicBoolean(false);

        // Highest priority cancels the event
        eventBusDispatcher.addListener(
                TestCancellableBusEvent.class,
                NeoForgeEventPriority.HIGHEST,
                false,
                ev -> ((TestCancellableBusEvent) ev).setCanceled(true),
                "canceler-sub"
        );

        // Normal listener with receiveCancelled = false should NOT be called
        eventBusDispatcher.addListener(
                TestCancellableBusEvent.class,
                NeoForgeEventPriority.NORMAL,
                false,
                ev -> normalInvoked.set(true),
                "normal-sub"
        );

        // Low listener with receiveCancelled = true SHOULD still be called
        eventBusDispatcher.addListener(
                TestCancellableBusEvent.class,
                NeoForgeEventPriority.LOW,
                true,
                ev -> lowWithReceiveCancelledInvoked.set(true),
                "cleanup-sub"
        );

        TestCancellableBusEvent event = new TestCancellableBusEvent("attack_event");
        boolean uncanceled = eventBusDispatcher.post(event);

        assertThat(uncanceled).isFalse();
        assertThat(event.isCanceled()).isTrue();
        assertThat(normalInvoked.get()).isFalse();
        assertThat(lowWithReceiveCancelledInvoked.get()).isTrue();
    }

    @Test
    @DisplayName("NeoForge EventBus 8 - Unregisters subscribers by ID successfully")
    public void testSubscriberUnregister() {
        AtomicInteger callCount = new AtomicInteger(0);

        eventBusDispatcher.addListener(
                TestSimpleBusEvent.class,
                ev -> callCount.incrementAndGet(),
                "transient-subscriber"
        );

        eventBusDispatcher.post(new TestSimpleBusEvent("tick-1"));
        assertThat(callCount.get()).isEqualTo(1);

        int removed = eventBusDispatcher.unregister("transient-subscriber");
        assertThat(removed).isEqualTo(1);

        eventBusDispatcher.post(new TestSimpleBusEvent("tick-2"));
        assertThat(callCount.get()).isEqualTo(1); // Not incremented again
    }
}
