package net.neoforged.neoforge.event.tick;

/**
 * NeoForge server tick event ({@code net.neoforged.neoforge.event.tick.ServerTickEvent}) fired on
 * the NeoForge game event bus before and after each server tick.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ServerTickEvent {

    private final boolean postTick;
    private final long tickNumber;

    /**
     * Constructs a new NeoForge {@code ServerTickEvent}.
     *
     * @param postTick   {@code false} for pre-tick phase, {@code true} for post-tick phase
     * @param tickNumber monotonic server tick number
     */
    public ServerTickEvent(boolean postTick, long tickNumber) {
        this.postTick = postTick;
        this.tickNumber = tickNumber;
    }

    /**
     * Returns whether this event represents the pre-tick phase.
     *
     * @return {@code true} if pre-tick
     */
    public boolean isPreTick() {
        return !postTick;
    }

    /**
     * Returns whether this event represents the post-tick phase.
     *
     * @return {@code true} if post-tick
     */
    public boolean isPostTick() {
        return postTick;
    }

    /**
     * Returns the monotonic server tick counter.
     *
     * @return server tick number
     */
    public long getTickNumber() {
        return tickNumber;
    }
}
