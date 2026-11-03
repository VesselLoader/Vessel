package net.minecraftforge.event;

/**
 * Forge server tick event ({@code net.minecraftforge.event.ServerTickEvent}) fired on the Forge
 * event bus at the start and end of each server tick.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ServerTickEvent extends TickEvent {

    private final long tickNumber;

    /**
     * Constructs a new {@code ServerTickEvent} for the given phase and monotonic tick counter.
     *
     * @param phase      tick phase ({@link TickPhase#START} or {@link TickPhase#END})
     * @param tickNumber monotonic server tick number
     */
    public ServerTickEvent(TickPhase phase, long tickNumber) {
        super("SERVER", phase);
        this.tickNumber = tickNumber;
    }

    /**
     * Returns the monotonic server tick counter associated with this tick event.
     *
     * @return server tick number
     */
    public long getTickNumber() {
        return tickNumber;
    }
}
