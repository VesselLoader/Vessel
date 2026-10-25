package dev.vessel.kernel.adapter.neoforge;

/**
 * Interface representing events that can have their outcome cancelled.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public interface NeoForgeCancellableEvent {

    /**
     * Determines whether the event has been marked as cancelled.
     *
     * @return true if cancelled
     */
    boolean isCanceled();

    /**
     * Sets the cancellation status of the event.
     *
     * @param canceled true to cancel
     */
    void setCanceled(boolean canceled);
}
