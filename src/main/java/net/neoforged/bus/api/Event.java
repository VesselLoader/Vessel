package net.neoforged.bus.api;

/**
 * Canonical base event class ({@code net.neoforged.bus.api.Event}) for the NeoForge
 * event bus subsystem.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class Event {

    private boolean canceled;

    public Event() {
    }

    /**
     * Determines whether this event can be cancelled by listeners.
     *
     * @return {@code true} if cancellable
     */
    public boolean isCancelable() {
        return false;
    }

    /**
     * Returns whether this event has been cancelled by an event listener.
     *
     * @return {@code true} if cancelled
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Sets the cancellation status of this event.
     *
     * @param cancel new cancellation status
     */
    public void setCanceled(boolean cancel) {
        if (!isCancelable() && cancel) {
            throw new UnsupportedOperationException("Attempted to call setCanceled() on an uncancellable event!");
        }
        this.canceled = cancel;
    }
}
