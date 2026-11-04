package dev.vessel.command;

import dev.vessel.bootstrap.target.VesselServerLauncher;
import java.util.Objects;

/**
 * Event-driven task that hooks into Minecraft server startup to register mod items, mod blocks,
 * and the VESSEL Brigadier command suite as soon as the server or commands dispatcher is ready.
 *
 * <p>Operates entirely through {@link VesselServerReadyCallback} with zero sleep loops and zero polling.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.8
 */
public final class VesselBrigadierRegistrationTask implements Runnable {

    private final ClassLoader tierOneClassLoader;
    private final VesselServerLauncher launcher;
    private final VesselBrigadierCommandBridge bridge;

    public VesselBrigadierRegistrationTask(
            ClassLoader tierOneClassLoader,
            VesselServerLauncher launcher,
            VesselBrigadierCommandBridge bridge) {
        this.tierOneClassLoader = Objects.requireNonNull(tierOneClassLoader, "tierOneClassLoader must not be null");
        this.launcher = Objects.requireNonNull(launcher, "launcher must not be null");
        this.bridge = Objects.requireNonNull(bridge, "bridge must not be null");
    }

    @Override
    public void run() {
        VesselBrigadierLifecycleRegistrar registrar =
                new VesselBrigadierLifecycleRegistrar(tierOneClassLoader, launcher, bridge);
        VesselServerBridgeRegistry.registerServerReadyCallback(registrar);
    }
}
