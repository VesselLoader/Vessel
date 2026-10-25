package dev.vessel.kernel.adapter.neoforge;

import java.util.Objects;

/**
 * Exception thrown when NeoForge EventBus 8 dispatch or subscriber invocation encounters fatal errors.
 * <p>
 * Eliminates ERR-96 (EventBus 8 concurrent listener mutation & priority inversion deadlock)
 * and addresses ECO-031 through ECO-040.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class NeoForgeEventBusException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final String eventType;
    private final String subscriberId;

    /**
     * Constructs an event bus exception with code, event type, subscriber ID and message.
     *
     * @param errorCode    error identifier
     * @param eventType    class name of the event
     * @param subscriberId identifier of the failing subscriber
     * @param message      explanatory message
     * @param cause        underlying exception
     */
    public NeoForgeEventBusException(
            String errorCode,
            String eventType,
            String subscriberId,
            String message,
            Throwable cause
    ) {
        super(String.format("[%s] Event '%s' in subscriber '%s': %s",
                errorCode, eventType, subscriberId, message), cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.subscriberId = Objects.requireNonNull(subscriberId, "subscriberId must not be null");
    }

    /**
     * Returns the error code.
     *
     * @return error code
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Returns the event type.
     *
     * @return event type
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * Returns the subscriber ID.
     *
     * @return subscriber ID
     */
    public String getSubscriberId() {
        return subscriberId;
    }
}
