package dev.vessel.kernel.adapter.neoforge;

import java.util.Objects;

/**
 * Standard non-cancellable test event fixture for NeoForge EventBus tests.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class TestSimpleBusEvent {

    private final String message;

    /**
     * Constructs a simple bus event with a message.
     *
     * @param message payload message
     */
    public TestSimpleBusEvent(String message) {
        this.message = Objects.requireNonNull(message, "message must not be null");
    }

    /**
     * Returns the message.
     *
     * @return message string
     */
    public String getMessage() {
        return message;
    }
}
