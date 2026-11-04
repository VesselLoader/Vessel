package dev.vessel.command;

import dev.vessel.bootstrap.mojang.VesselUniversalModBlockRegistryBridge;
import dev.vessel.bootstrap.mojang.VesselUniversalModItemRegistryBridge;
import dev.vessel.bootstrap.target.VesselServerLauncher;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Event-driven lifecycle registrar executing mod item injection, mod block injection, and
 * Brigadier command suite registration immediately when the server or command dispatcher is ready.
 *
 * <p>Completely replaces the obsolete 300-second polling thread loop with zero latency and zero race conditions.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselBrigadierLifecycleRegistrar implements VesselServerReadyCallback {

    private final ClassLoader tierOneClassLoader;
    private final VesselServerLauncher launcher;
    private final VesselBrigadierCommandBridge bridge;
    private final VesselLegacyCommandBridge legacyBridge = new VesselLegacyCommandBridge();
    private final DynamicBukkitCommandBrigadierAdapter bukkitBrigadierAdapter =
            new DynamicBukkitCommandBrigadierAdapter();
    private final ForgeDeferredCommandRegistrationBridge forgeCommandsBridge =
            new ForgeDeferredCommandRegistrationBridge();
    private final dev.vessel.kernel.lifecycle.CrossEraServerLifecycleSyncHook lifecycleSyncHook =
            new dev.vessel.kernel.lifecycle.CrossEraServerLifecycleSyncHook();
    private final VesselUniversalModItemRegistryBridge itemBridge =
            new VesselUniversalModItemRegistryBridge();
    private final VesselUniversalModBlockRegistryBridge blockBridge =
            new VesselUniversalModBlockRegistryBridge();
    private final AtomicBoolean registered = new AtomicBoolean(false);

    public VesselBrigadierLifecycleRegistrar(
            ClassLoader tierOneClassLoader,
            VesselServerLauncher launcher,
            VesselBrigadierCommandBridge bridge) {
        this.tierOneClassLoader = Objects.requireNonNull(tierOneClassLoader, "tierOneClassLoader must not be null");
        this.launcher = Objects.requireNonNull(launcher, "launcher must not be null");
        this.bridge = Objects.requireNonNull(bridge, "bridge must not be null");
    }

    @Override
    public void onServerOrCommandsReady(Object serverOrCommands) {
        Objects.requireNonNull(serverOrCommands, "serverOrCommands must not be null");
        if (!registered.compareAndSet(false, true)) {
            return;
        }

        Path serverRoot = launcher.getServerRoot();

        // 1. Ingest and inject all real mod items into native item registry across eras
        try {
            itemBridge.injectAllModItemsIntoRegistry(tierOneClassLoader, serverRoot);
        } catch (Throwable t) {
            System.err.println("[VESSEL/WARN] Error injecting mod items into registry: " + t.getMessage());
        }

        // 2. Ingest and inject all real mod blocks into native block registry across eras
        try {
            blockBridge.injectAllModBlocksIntoRegistry(tierOneClassLoader, serverRoot);
        } catch (Throwable t) {
            System.err.println("[VESSEL/WARN] Error injecting mod blocks into registry: " + t.getMessage());
        }

        // 3. Register commands suite
        try {
            if (legacyBridge.isLegacyEra(tierOneClassLoader)) {
                legacyBridge.registerIntoLegacyCommandManager(
                        serverOrCommands,
                        tierOneClassLoader,
                        launcher);
            } else {
                boolean ok = bridge.registerIntoBrigadier(serverOrCommands, tierOneClassLoader, launcher);
                int bukkitCount = bukkitBrigadierAdapter.registerAllBukkitCommandsIntoBrigadier(
                        serverOrCommands,
                        tierOneClassLoader);
                boolean forgeDispatched = forgeCommandsBridge.dispatchRegisterCommandsEvent(
                        serverOrCommands,
                        tierOneClassLoader);
                lifecycleSyncHook.onServerStarting(serverOrCommands);
                lifecycleSyncHook.onServerStarted(serverOrCommands);

                System.out.println("[VESSEL/INFO] Event-driven Brigadier command suite registered: sovereign="
                        + ok + ", bukkitCommands=" + bukkitCount + ", forgeDispatched=" + forgeDispatched);
                VesselServerBridgeRegistry.sendCommandsToAllPlayers();
            }
        } catch (Throwable t) {
            System.err.println("[VESSEL/ERROR] Error registering Brigadier command suite: " + t.getMessage());
        }
    }

    /**
     * Returns whether the registration has already executed.
     *
     * @return {@code true} if completed
     */
    public boolean isRegistered() {
        return registered.get();
    }
}
