package dev.vessel.kernel.lifecycle;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe global lifecycle state tracker for the VESSEL server microkernel,
 * allowing test runners and internal subsystems to know deterministically whether the
 * Minecraft dedicated server has reached the {@code READY} state before dispatching commands.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselServerLifecycleStateTracker {

    public static final String STATE_BOOTING = "BOOTING";
    public static final String STATE_STARTING = "STARTING";
    public static final String STATE_READY = "READY";
    public static final String STATE_STOPPING = "STOPPING";

    private static final AtomicReference<String> CURRENT_STATE =
            new AtomicReference<>(STATE_BOOTING);

    private static volatile Object activeServerInstance;

    public VesselServerLifecycleStateTracker() {
    }

    /**
     * Marks the server state as {@code STARTING}.
     *
     * @param server active Minecraft server instance
     */
    public static void markServerStarting(Object server) {
        activeServerInstance = server;
        CURRENT_STATE.set(STATE_STARTING);
    }

    /**
     * Marks the server state as {@code READY} and emits the canonical deterministic readiness banner.
     *
     * @param server active Minecraft server instance
     */
    public static void markServerReady(Object server) {
        activeServerInstance = server;
        CURRENT_STATE.set(STATE_READY);
        System.out.println("[VESSEL/READY] Server is fully READY! Port 25565 listening, console accepting commands.");
    }

    /**
     * Marks the server state as {@code STOPPING}.
     */
    public static void markServerStopping() {
        CURRENT_STATE.set(STATE_STOPPING);
    }

    /**
     * Returns whether the server is currently in the {@code READY} state.
     *
     * @return {@code true} if server is ready for command execution
     */
    public static boolean isServerReady() {
        return STATE_READY.equals(CURRENT_STATE.get());
    }

    /**
     * Returns the active server state code.
     *
     * @return state code string
     */
    public static String getCurrentState() {
        return CURRENT_STATE.get();
    }

    /**
     * Returns the active Minecraft server instance if registered.
     *
     * @return server instance or {@code null}
     */
    public static Object getActiveServerInstance() {
        return activeServerInstance;
    }
}
