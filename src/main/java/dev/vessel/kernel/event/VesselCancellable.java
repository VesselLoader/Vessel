package dev.vessel.kernel.event;

/**
 * Interface implemented by events that support execution cancellation.
 * <p>
 * Unifies Bukkit {@code Cancellable} and Forge/NeoForge {@code ICancellableEvent}.
 * When cancelled, subsequent non-monitor listeners may be skipped based on registration flags.
 *
 * @version 1.0.0
 */
public interface VesselCancellable {

    /**
     * Checks if this event has been cancelled by an earlier listener.
     *
     * @return true if cancelled
     */
    boolean isCancelled();

    /**
     * Sets the cancellation status of this event.
     *
     * @param cancel true to cancel the event
     */
    void setCancelled(boolean cancel);
}
