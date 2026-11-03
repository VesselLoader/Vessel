package dev.vessel.bootstrap.wave1;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

/**
 * Sample Bukkit {@link Listener} fixture verifying {@link EventHandler} priority dispatch
 * ({@code LOWEST}, {@code NORMAL}, {@code HIGH}, {@code MONITOR}), {@code ignoreCancelled}
 * filtering, {@code MONITOR} read-only cancellation restoration, and {@link ServerLoadEvent}
 * reception.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class SampleBukkitWaveOneListenerFixture implements Listener {

    private final List<String> observedServerLoadTypes = new ArrayList<>();

    public SampleBukkitWaveOneListenerFixture() {
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMonitorAttemptIllegalUncancel(SampleBukkitWaveOneCancelableEvent event) {
        event.recordStep("MONITOR");
        // Attempt to mutate cancellation during MONITOR phase; ReflectiveBukkitEventConsumer
        // must restore the pre-MONITOR cancelled state back to true per ADR-0003.
        event.setCancelled(false);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHighIgnoredWhenCancelled(SampleBukkitWaveOneCancelableEvent event) {
        event.recordStep("HIGH_IGNORED");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onHighReceivedDespiteCancelled(SampleBukkitWaveOneCancelableEvent event) {
        event.recordStep("HIGH_RECEIVED");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onNormalCancelEvent(SampleBukkitWaveOneCancelableEvent event) {
        event.recordStep("NORMAL");
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLowestFirst(SampleBukkitWaveOneCancelableEvent event) {
        event.recordStep("LOWEST");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onServerLoad(ServerLoadEvent event) {
        observedServerLoadTypes.add(event.getLoadType());
    }

    public List<String> getObservedServerLoadTypes() {
        return observedServerLoadTypes;
    }
}
