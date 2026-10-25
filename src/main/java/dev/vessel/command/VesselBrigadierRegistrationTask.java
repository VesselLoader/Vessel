package dev.vessel.command;

import dev.vessel.bootstrap.mojang.VesselUniversalModItemRegistryBridge;
import dev.vessel.bootstrap.target.VesselServerLauncher;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Set;

/**
 * Background daemon task that monitors the live Minecraft server startup, injects discovered
 * mod items into {@code BuiltInRegistries.ITEM}, and registers the VESSEL Brigadier command suite
 * as soon as the server's {@code Commands} dispatcher or {@code MinecraftServer} instance is active.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.3
 */
public final class VesselBrigadierRegistrationTask implements Runnable {

    private final ClassLoader tierOneClassLoader;
    private final VesselServerLauncher launcher;
    private final VesselBrigadierCommandBridge bridge;
    private final VesselLegacyCommandBridge legacyBridge = new VesselLegacyCommandBridge();

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
        VesselUniversalModItemRegistryBridge itemBridge = new VesselUniversalModItemRegistryBridge();
        boolean registered = false;
        for (int i = 0; i < 300; i++) {
            try {
                Thread.sleep(1000L);

                Object serverInstance = VesselServerBridgeRegistry.getActiveMinecraftServer();
                if (serverInstance == null) {
                    serverInstance = VesselServerBridgeRegistry.getActiveCommands();
                }
                if (serverInstance == null) {
                    serverInstance = VesselServerBridgeRegistry.getActiveLegacyCommandManager();
                }
                if (serverInstance == null) {
                    Class<?> serverClass = Class.forName(
                            "net.minecraft.server.MinecraftServer",
                            false,
                            tierOneClassLoader);
                    serverInstance = findActiveServerInstance(serverClass);
                }

                if (serverInstance != null && !registered) {
                    // 1. Inject all mod items into native item registry across all eras
                    itemBridge.injectAllModItemsIntoRegistry(tierOneClassLoader, launcher.getServerRoot());

                    // 2. Register all commands: check for legacy pre-1.13 era vs modern Brigadier era
                    if (legacyBridge.isLegacyEra(tierOneClassLoader)) {
                        registered = legacyBridge.registerIntoLegacyCommandManager(
                                serverInstance,
                                tierOneClassLoader,
                                launcher);
                    } else {
                        boolean ok = bridge.registerIntoBrigadier(serverInstance, tierOneClassLoader, launcher);
                        if (ok) {
                            registered = true;
                            VesselServerBridgeRegistry.sendCommandsToAllPlayers();
                        }
                    }
                }

                // If registered, periodically re-broadcast commands when players are online to guarantee sync
                if (registered) {
                    int sent = VesselServerBridgeRegistry.sendCommandsToAllPlayers();
                    if (sent > 0 && i > 15) {
                        // After initial player join sync, break daemon loop
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ignored) {
            }
        }
    }

    private static Object findActiveServerInstance(Class<?> serverClass) {
        Object direct = VesselServerBridgeRegistry.getActiveMinecraftServer();
        if (direct != null) {
            return direct;
        }

        try {
            Set<Thread> threads = Thread.getAllStackTraces().keySet();
            for (Thread t : threads) {
                if ("Server thread".equals(t.getName())) {
                    if (serverClass.isInstance(t)) {
                        return t;
                    }
                    try {
                        Field targetField = findTargetField(t.getClass());
                        if (targetField != null) {
                            targetField.setAccessible(true);
                            Object target = targetField.get(t);
                            if (target != null && serverClass.isInstance(target)) {
                                return target;
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Field findTargetField(Class<?> clazz) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField("target");
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
