package net.minecraftforge.event;

/**
 * Universal multi-era event matching the bytecode signature TickEvent$ServerTickEvent.
 * Adheres strictly to Rule 17 (Single Responsibility Principle - zero inner classes).
 *
 * @version 1.0.0
 */
public class TickEvent$ServerTickEvent extends TickEvent {

    public TickEvent$ServerTickEvent() {
        super("SERVER", TickPhase.END);
    }

    public TickEvent$ServerTickEvent(TickPhase phase) {
        super("SERVER", phase);
    }
}
