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
 * @version 1.0.1
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
        VesselUniversalModItemRegistryBridge itemBridge = new VesselUniversalModItemRegistryBridge();
        for (int i = 0; i < 60; i++) {
            try {
                Thread.sleep(1000L);

                Object serverInstance = VesselServerBridgeRegistry.getActiveMinecraftServer();
                if (serverInstance == null) {
                    serverInstance = VesselServerBridgeRegistry.getActiveCommands();
                }
                if (serverInstance == null) {
                    Class<?> serverClass = Class.forName(
                            "net.minecraft.server.MinecraftServer",
                            false,
                            tierOneClassLoader);
                    serverInstance = findActiveServerInstance(serverClass);
                }

                if (serverInstance != null) {
                    // 1. Inject all mod items (Create, etc.) into BuiltInRegistries.ITEM
                    itemBridge.injectAllModItemsIntoRegistry(tierOneClassLoader, launcher.getServerRoot());

                    // 2. Register all commands into Brigadier dispatcher
                    boolean ok = bridge.registerIntoBrigadier(serverInstance, tierOneClassLoader, launcher);
                    if (ok) {
                        VesselServerBridgeRegistry.sendCommandsToAllPlayers();
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
