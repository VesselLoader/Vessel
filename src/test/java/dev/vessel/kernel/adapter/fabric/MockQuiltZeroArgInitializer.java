package dev.vessel.kernel.adapter.fabric;

/**
 * Test mock for Quilt ModInitializer with zero-arg fallback.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class MockQuiltZeroArgInitializer {

    public static volatile boolean initialized = false;

    public MockQuiltZeroArgInitializer() {
    }

    public void onInitialize() {
        initialized = true;
    }
}
