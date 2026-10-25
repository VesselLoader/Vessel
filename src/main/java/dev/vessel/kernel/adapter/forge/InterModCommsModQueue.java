package dev.vessel.kernel.adapter.forge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

/**
 * Thread-safe message queue managing inbound IMC messages destined for a specific mod.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class InterModCommsModQueue {

    private final String targetModId;
    private final ConcurrentLinkedQueue<InterModCommsMessage> messages = new ConcurrentLinkedQueue<>();

    /**
     * Constructs a queue dedicated to a recipient mod ID.
     *
     * @param targetModId target mod identifier
     */
    public InterModCommsModQueue(String targetModId) {
        this.targetModId = Objects.requireNonNull(targetModId, "targetModId must not be null");
    }

    /**
     * Returns the target mod ID.
     *
     * @return target mod ID
     */
    public String getTargetModId() {
        return targetModId;
    }

    /**
     * Enqueues an inbound message for the mod.
     *
     * @param message the IMC message
     */
    public void enqueue(InterModCommsMessage message) {
        Objects.requireNonNull(message, "message must not be null");
        messages.add(message);
    }

    /**
     * Retrieves an unmodifiable snapshot of all pending messages.
     *
     * @return list of messages
     */
    public List<InterModCommsMessage> getAllMessages() {
        return Collections.unmodifiableList(new ArrayList<>(messages));
    }

    /**
     * Filters messages matching a specific communication method name.
     *
     * @param method method identifier
     * @return unmodifiable list of matching messages
     */
    public List<InterModCommsMessage> getMessagesByMethod(String method) {
        Objects.requireNonNull(method, "method must not be null");
        String normalized = method.trim().toLowerCase(Locale.ROOT);
        List<InterModCommsMessage> result = new ArrayList<>();
        for (InterModCommsMessage msg : messages) {
            if (msg.method().trim().equalsIgnoreCase(normalized)) {
                result.add(msg);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Drains and returns all queued messages.
     *
     * @return list of drained messages
     */
    public List<InterModCommsMessage> drainAll() {
        List<InterModCommsMessage> result = new ArrayList<>();
        InterModCommsMessage msg;
        while ((msg = messages.poll()) != null) {
            result.add(msg);
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Provides a stream of current queued messages.
     *
     * @return message stream
     */
    public Stream<InterModCommsMessage> stream() {
        return messages.stream();
    }

    /**
     * Returns current queued message count.
     *
     * @return count
     */
    public int size() {
        return messages.size();
    }
}
