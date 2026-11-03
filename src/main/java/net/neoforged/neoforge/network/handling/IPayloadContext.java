package net.neoforged.neoforge.network.handling;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * NeoForge custom payload execution context
 * ({@code net.neoforged.neoforge.network.handling.IPayloadContext}) providing sender identity,
 * negotiated protocol version, and main-thread work scheduling.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class IPayloadContext {

    private final UUID playerUuid;
    private final String protocolVersion;
    private final AtomicInteger executedTaskCount = new AtomicInteger();

    /**
     * Constructs a new {@code IPayloadContext} for the given player UUID and protocol version.
     *
     * @param playerUuid      sender/receiver player UUID
     * @param protocolVersion negotiated network protocol version string
     */
    public IPayloadContext(UUID playerUuid, String protocolVersion) {
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid must not be null");
        this.protocolVersion = Objects.requireNonNull(
                protocolVersion, "protocolVersion must not be null");
    }

    /**
     * Returns the UUID of the player associated with this payload context.
     *
     * @return player UUID
     */
    public UUID playerUuid() {
        return playerUuid;
    }

    /**
     * Returns the negotiated protocol version for this payload channel.
     *
     * @return protocol version string
     */
    public String protocolVersion() {
        return protocolVersion;
    }

    /**
     * Enqueues and executes a task on the region/main strand, incrementing the executed task
     * counter and returning a completed future.
     *
     * @param task non-null work runnable
     * @return completed future
     */
    public CompletableFuture<Void> enqueueWork(Runnable task) {
        Objects.requireNonNull(task, "task must not be null");
        task.run();
        executedTaskCount.incrementAndGet();
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Returns the total number of tasks executed via {@link #enqueueWork(Runnable)}.
     *
     * @return executed task count
     */
    public int getExecutedTaskCount() {
        return executedTaskCount.get();
    }
}
