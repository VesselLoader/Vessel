package dev.vessel.kernel.adapter.fabric;

/**
 * Test mock for Quilt ModInitializer accepting a ModContainer.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class MockQuiltModInitializer {

    public static volatile boolean initialized = false;
    public static volatile Object passedContainer = null;

    public MockQuiltModInitializer() {
    }

    public void onInitialize(Object modContainer) {
        initialized = true;
        passedContainer = modContainer;
    }
}
