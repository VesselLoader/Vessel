package dev.vessel.kernel.adapter.fabric;

import dev.vessel.kernel.event.VesselCancellable;

/**
 * Test mock event implementing {@link VesselCancellable}.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class MockCancellableEvent implements VesselCancellable {

    private boolean cancelled = false;
    private final String payload;

    public MockCancellableEvent(String payload) {
        this.payload = payload;
    }

    public String getPayload() {
        return payload;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
