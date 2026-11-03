package org.bukkit.event.world;

import java.util.Objects;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Canonical Bukkit {@code ChunkLoadEvent} fired when a {@link Chunk} is loaded into a
 * {@link World}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ChunkLoadEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Chunk chunk;
    private final boolean newChunk;

    public ChunkLoadEvent(Chunk chunk, boolean newChunk) {
        this.chunk = Objects.requireNonNull(chunk, "chunk must not be null");
        this.newChunk = newChunk;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public World getWorld() {
        return chunk.getWorld();
    }

    public boolean isNewChunk() {
        return newChunk;
    }

    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
