package dev.vessel.kernel.adapter.fabric;

/**
 * Test mock for entrypoints that throw exceptions.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class MockFailingInitializer {

    public MockFailingInitializer() {
    }

    public void onInitialize() {
        throw new IllegalStateException("Simulated initialization fault in mock");
    }
}
