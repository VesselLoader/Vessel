package dev.vessel.bootstrap.wave1;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Sample cancelable Bukkit {@link Event} fixture used in Wave 1 tests to verify
 * {@code @EventHandler} priority ordering, {@code ignoreCancelled} semantics, and
 * {@code MONITOR} phase read-only cancellation enforcement.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class SampleBukkitWaveOneCancelableEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private boolean cancelled;
    private final List<String> executionOrder = new ArrayList<>();

    public SampleBukkitWaveOneCancelableEvent() {
        super(false);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    public void recordStep(String stepName) {
        this.executionOrder.add(stepName);
    }

    public List<String> getExecutionOrder() {
        return executionOrder;
    }

    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
