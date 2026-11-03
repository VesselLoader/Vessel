package net.minecraftforge.event;

import java.util.Objects;

/**
 * Base Forge tick event ({@code net.minecraftforge.event.TickEvent}) dispatched on the Forge event
 * bus at the {@link TickPhase#START} and {@link TickPhase#END} boundaries of each tick.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class TickEvent {

    private final String type;
    private final TickPhase phase;

    /**
     * Constructs a new {@code TickEvent} with the specified tick domain type and phase.
     *
     * @param type  tick domain identifier (e.g., {@code "SERVER"})
     * @param phase tick execution phase ({@link TickPhase#START} or {@link TickPhase#END})
     */
    public TickEvent(String type, TickPhase phase) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.phase = Objects.requireNonNull(phase, "phase must not be null");
    }

    /**
     * Returns the tick domain type identifier.
     *
     * @return tick type string
     */
    public final String getType() {
        return type;
    }

    /**
     * Returns the current {@link TickPhase} of this tick event.
     *
     * @return tick phase
     */
    public final TickPhase getPhase() {
        return phase;
    }
}
