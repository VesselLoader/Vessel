package dev.vessel.kernel.adapter.fabric;

/**
 * Test mock for Fabric common ModInitializer.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class MockFabricModInitializer {

    public static volatile boolean initialized = false;

    public MockFabricModInitializer() {
    }

    public void onInitialize() {
        initialized = true;
    }
}
