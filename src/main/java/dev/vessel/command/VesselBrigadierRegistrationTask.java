package dev.vessel.command;

import dev.vessel.bootstrap.target.VesselServerLauncher;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Background daemon task that monitors the live Minecraft server startup and registers VESSEL
 * Brigadier commands as soon as the server's {@code Commands} dispatcher is ready.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
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
        for (int i = 0; i < 60; i++) {
            try {
                Thread.sleep(1000L);
                Class<?> serverClass = Class.forName(
                        "net.minecraft.server.MinecraftServer",
                        false,
                        tierOneClassLoader);
                Object serverInstance = findActiveServerInstance(serverClass);
                if (serverInstance != null) {
                    boolean ok = bridge.registerIntoBrigadier(serverInstance, tierOneClassLoader, launcher);
                    if (ok) {
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
        try {
            for (Method m : serverClass.getMethods()) {
                if (m.getParameterCount() == 0 && m.getReturnType().equals(serverClass)) {
                    Object inst = m.invoke(null);
                    if (inst != null) {
                        return inst;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
