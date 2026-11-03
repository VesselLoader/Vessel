package org.bukkit.event;

/**
 * Contract for Bukkit/Spigot/Paper/Folia events that may be cancelled by an event listener prior
 * to the {@link EventPriority#MONITOR} observation tier.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public interface Cancellable {

    /**
     * Gets the cancellation state of this event.
     *
     * @return {@code true} if this event is currently cancelled
     */
    boolean isCancelled();

    /**
     * Sets the cancellation state of this event.
     *
     * @param cancel {@code true} if you wish to cancel this event
     */
    void setCancelled(boolean cancel);
}
