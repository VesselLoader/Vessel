package dev.vessel.kernel.adapter.neoforge;

import java.util.Objects;

/**
 * Immutable holder encapsulating an event subscriber along with its priority and cancellation handling policy.
 *
 * @param priority         priority tier of the subscriber
 * @param receiveCancelled whether this subscriber should still be called if the event is already cancelled
 * @param subscriber       the execution delegate
 * @param subscriberId     unique diagnostic identifier
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record NeoForgePrioritizedSubscriber(
        NeoForgeEventPriority priority,
        boolean receiveCancelled,
        NeoForgeEventSubscriber subscriber,
        String subscriberId
) {

    /**
     * Compact constructor validating non-null arguments.
     */
    public NeoForgePrioritizedSubscriber {
        Objects.requireNonNull(priority, "priority must not be null");
        Objects.requireNonNull(subscriber, "subscriber must not be null");
        Objects.requireNonNull(subscriberId, "subscriberId must not be null");
    }
}
