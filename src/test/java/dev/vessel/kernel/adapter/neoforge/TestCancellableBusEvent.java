package dev.vessel.kernel.adapter.neoforge;

/**
 * Concrete test fixture representing a cancellable event for NeoForge EventBus tests.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class TestCancellableBusEvent implements NeoForgeCancellableEvent {

    private final String eventPayload;
    private boolean canceled;

    /**
     * Constructs a cancellable test event.
     *
     * @param eventPayload test payload
     */
    public TestCancellableBusEvent(String eventPayload) {
        this.eventPayload = eventPayload;
        this.canceled = false;
    }

    /**
     * Returns the payload string.
     *
     * @return payload
     */
    public String getEventPayload() {
        return eventPayload;
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    @Override
    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
}
