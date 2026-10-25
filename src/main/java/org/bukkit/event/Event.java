package org.bukkit.event;

/**
 * Canonical base class for all Bukkit, Spigot, Paper, and Folia runtime events dispatched through
 * the VESSEL Sovereign Microkernel.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class Event {

    private final boolean asynchronous;

    /**
     * Constructs a synchronous Bukkit event.
     */
    public Event() {
        this(false);
    }

    /**
     * Constructs a Bukkit event with explicit concurrency mode.
     *
     * @param isAsync {@code true} if the event is fired asynchronously
     */
    public Event(boolean isAsync) {
        this.asynchronous = isAsync;
    }

    /**
     * Returns the canonical simple name of this event class.
     *
     * @return the event name
     */
    public String getEventName() {
        return getClass().getSimpleName();
    }

    /**
     * Returns whether this event is fired asynchronously off region strands.
     *
     * @return {@code true} if asynchronous
     */
    public final boolean isAsynchronous() {
        return asynchronous;
    }
}
