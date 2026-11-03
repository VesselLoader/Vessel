package net.minecraftforge.event;

/**
 * Execution phase within a single Minecraft Forge tick cycle
 * ({@code net.minecraftforge.event.TickPhase}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public enum TickPhase {

    /**
     * Fired at the beginning of the tick before logic updates execute.
     */
    START,

    /**
     * Fired at the conclusion of the tick after logic updates complete.
     */
    END
}
