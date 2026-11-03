package org.bukkit.event.world;

import java.util.Objects;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Canonical Bukkit {@code ChunkUnloadEvent} fired when a {@link Chunk} is unloaded from a
 * {@link World}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ChunkUnloadEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Chunk chunk;
    private volatile boolean saveChunk = true;

    public ChunkUnloadEvent(Chunk chunk) {
        this.chunk = Objects.requireNonNull(chunk, "chunk must not be null");
    }

    public Chunk getChunk() {
        return chunk;
    }

    public World getWorld() {
        return chunk.getWorld();
    }

    public boolean isSaveChunk() {
        return saveChunk;
    }

    public void setSaveChunk(boolean saveChunk) {
        this.saveChunk = saveChunk;
    }

    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
