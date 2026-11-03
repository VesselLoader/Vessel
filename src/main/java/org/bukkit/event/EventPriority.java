package org.bukkit.event;

/**
 * Represents the execution priority of a Bukkit/Spigot/Paper/Folia event handler method.
 *
 * <p>Listeners with lower priority slots ({@link #LOWEST}) are executed first, while higher
 * priority slots ({@link #HIGHEST}) have the final say on event outcome before {@link #MONITOR}
 * listeners inspect the final result strictly for read-only observation.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public enum EventPriority {

    /**
     * Event call is of very low importance and should be run first, to allow other plugins to
     * further customize the outcome.
     */
    LOWEST(0),

    /**
     * Event call is of low importance.
     */
    LOW(1),

    /**
     * Event call is neither important nor unimportant, and may be run normally.
     */
    NORMAL(2),

    /**
     * Event call is of high importance.
     */
    HIGH(3),

    /**
     * Event call is critical and must have the final say in what happens to the event.
     */
    HIGHEST(4),

    /**
     * Event is listened to purely for monitoring the outcome of an event. No modifications to the
     * event should be made under this priority.
     */
    MONITOR(5);

    private final int slot;

    EventPriority(int slot) {
        this.slot = slot;
    }

    /**
     * Returns the numeric execution slot of this priority ({@code 0..5}).
     *
     * @return the priority slot index
     */
    public int getSlot() {
        return slot;
    }
}
