package dev.vessel.kernel.actor;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Lockless Multi-Producer Single-Consumer (MPSC) message mailbox for Actor Strands.
 * <p>
 * Provides high-throughput, non-blocking task submission across concurrent threads
 * without thread contention or monitor locks.
 * </p>
 *
 * @param <T> message or task type
 * @version 1.0.2
 * @since 1.0.0 (Pass K-4)
 */
public class ActorMailboxQueue<T> {

    private final ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<>();

    /**
     * Enqueues a message into the mailbox. Thread-safe for any number of concurrent producers.
     *
     * @param message the message or task to enqueue
     */
    public void send(T message) {
        Objects.requireNonNull(message, "message cannot be null");
        queue.offer(message);
    }

    /**
     * Polls the next available message in the mailbox.
     *
     * @return an Optional containing the message if present, or empty Optional if mailbox is empty
     */
    public Optional<T> poll() {
        return Optional.ofNullable(queue.poll());
    }

    /**
     * Drains all pending messages currently in the mailbox into the specified consumer.
     *
     * @param consumer the handler invoked for each drained message
     * @return the number of messages drained
     */
    public int drainTo(Consumer<T> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        int count = 0;
        T item;
        while ((item = queue.poll()) != null) {
            consumer.accept(item);
            count++;
        }
        return count;
    }

    /**
     * Drains up to a bounded maximum batch of messages from the mailbox.
     * <p>
     * Prevents strand task starvation and ensures strict time-slice fairness across regions
     * during high-burst transaction spikes.
     * </p>
     *
     * @param consumer message handler
     * @param maxBatchSize maximum number of messages to process in this time slice
     * @return number of messages drained
     */
    public int drainBatch(Consumer<T> consumer, int maxBatchSize) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        if (maxBatchSize <= 0) {
            return 0;
        }
        int count = 0;
        T item;
        while (count < maxBatchSize && (item = queue.poll()) != null) {
            consumer.accept(item);
            count++;
        }
        return count;
    }

    /**
     * Returns true if the mailbox currently contains no messages.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Returns the approximate number of messages currently in the mailbox.
     *
     * @return approximate message count
     */
    public int size() {
        return queue.size();
    }
}
