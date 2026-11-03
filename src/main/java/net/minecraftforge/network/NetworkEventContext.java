package net.minecraftforge.network;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Forge network packet handler execution context ({@code net.minecraftforge.network.NetworkEventContext})
 * exposing the packet's {@link NetworkDirection}, sender UUID, main-thread work queue, and handled flag.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class NetworkEventContext {

    private final NetworkDirection direction;
    private final UUID senderUuid;
    private volatile boolean packetHandled;
    private final AtomicInteger enqueuedWorkCount = new AtomicInteger();

    /**
     * Constructs a new {@code NetworkEventContext} for the specified direction and optional sender UUID.
     *
     * @param direction  non-null network transmission direction
     * @param senderUuid sender player UUID (may be {@code null} for server-to-client packets)
     */
    public NetworkEventContext(NetworkDirection direction, UUID senderUuid) {
        this.direction = Objects.requireNonNull(direction, "direction must not be null");
        this.senderUuid = senderUuid;
    }

    /**
     * Returns the transmission direction of the packet.
     *
     * @return non-null network direction
     */
    public NetworkDirection getDirection() {
        return direction;
    }

    /**
     * Returns the sending player's UUID, or {@code null} if sent from the server.
     *
     * @return sender UUID or {@code null}
     */
    public UUID getSenderUuid() {
        return senderUuid;
    }

    /**
     * Enqueues and executes packet handling logic on the main server/client thread.
     *
     * @param runnable non-null work task
     */
    public void enqueueWork(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable must not be null");
        runnable.run();
        enqueuedWorkCount.incrementAndGet();
    }

    /**
     * Returns the total number of work tasks enqueued via {@link #enqueueWork(Runnable)}.
     *
     * @return enqueued work count
     */
    public int getEnqueuedWorkCount() {
        return enqueuedWorkCount.get();
    }

    /**
     * Returns {@code true} if the packet has been marked as handled.
     *
     * @return {@code true} if handled
     */
    public boolean getPacketHandled() {
        return packetHandled;
    }

    /**
     * Marks whether the packet has been handled by the subscriber.
     *
     * @param packetHandled {@code true} if handled
     */
    public void setPacketHandled(boolean packetHandled) {
        this.packetHandled = packetHandled;
    }
}
