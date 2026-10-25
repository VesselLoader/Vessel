package dev.vessel.kernel.adapter.fabric;

/**
 * Test mock for Fabric dedicated server ModInitializer.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class MockFabricServerInitializer {

    public static volatile boolean initialized = false;

    public MockFabricServerInitializer() {
    }

    public void onInitializeServer() {
        initialized = true;
    }
}
