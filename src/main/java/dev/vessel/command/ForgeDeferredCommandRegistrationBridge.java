package dev.vessel.command;

import java.util.Objects;
import net.minecraftforge.common.MinecraftForge;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Bridges Minecraft DedicatedServer's native Brigadier {@code CommandDispatcher} into the Forge
 * ({@link MinecraftForge#EVENT_BUS}) and NeoForge ({@link NeoForge#EVENT_BUS}) event buses by
 * dynamically dispatching {@code RegisterCommandsEvent} when the server or Commands instance is ready,
 * and wiring direct mod commands via {@link DirectModCommandRegistrar}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.6
 */
public final class ForgeDeferredCommandRegistrationBridge {

    private final BrigadierDispatcherExtractor dispatcherExtractor = new BrigadierDispatcherExtractor();
    private final DirectModCommandRegistrar directRegistrar = new DirectModCommandRegistrar();
    private volatile boolean dispatched;

    /**
     * Attempts to dispatch {@code RegisterCommandsEvent} across Forge and NeoForge event buses.
     *
     * @param serverOrCommands   running MinecraftServer or Commands instance
     * @param tierOneClassLoader Tier-1 classloader containing Minecraft classes
     * @return {@code true} if commands event was posted to the Forge and NeoForge buses
     */
    public boolean dispatchRegisterCommandsEvent(
            Object serverOrCommands,
            ClassLoader tierOneClassLoader) {
        if (serverOrCommands == null || tierOneClassLoader == null) {
            return false;
        }

        Object dispatcher = dispatcherExtractor.extractDispatcher(serverOrCommands);
        if (dispatcher == null) {
            return false;
        }

        boolean successForge = postForgeRegisterCommands(dispatcher, tierOneClassLoader);
        boolean successNeo = postNeoForgeRegisterCommands(dispatcher, tierOneClassLoader);
        directRegistrar.registerDirectModCommands(dispatcher, tierOneClassLoader);

        ClassLoader modCl = VesselServerBridgeRegistry.getActiveModClassLoader();
        if (modCl != null && modCl != tierOneClassLoader) {
            directRegistrar.registerDirectModCommands(dispatcher, modCl);
        }

        ClassLoader contextCl = Thread.currentThread().getContextClassLoader();
        if (contextCl != null && contextCl != tierOneClassLoader && contextCl != modCl) {
            directRegistrar.registerDirectModCommands(dispatcher, contextCl);
        }

        this.dispatched = successForge || successNeo;
        return this.dispatched;
    }

    /**
     * Checks whether the deferred commands event has already been dispatched.
     *
     * @return {@code true} if dispatched
     */
    public boolean isDispatched() {
        return dispatched;
    }

    private boolean postForgeRegisterCommands(Object dispatcher, ClassLoader tierOneClassLoader) {
        try {
            net.minecraftforge.event.RegisterCommandsEvent event =
                    new net.minecraftforge.event.RegisterCommandsEvent(dispatcher);
            MinecraftForge.EVENT_BUS.post(event);
            return true;
        } catch (Throwable t) {
            System.err.println("[VESSEL] Notice: Forge RegisterCommandsEvent post: " + t.getMessage());
            t.printStackTrace();
            return false;
        }
    }

    private boolean postNeoForgeRegisterCommands(Object dispatcher, ClassLoader tierOneClassLoader) {
        try {
            net.neoforged.neoforge.event.RegisterCommandsEvent event =
                    new net.neoforged.neoforge.event.RegisterCommandsEvent(dispatcher);
            NeoForge.EVENT_BUS.post(event);
            return true;
        } catch (Throwable t) {
            System.err.println("[VESSEL] Notice: NeoForge RegisterCommandsEvent post: " + t.getMessage());
            t.printStackTrace();
            return false;
        }
    }

    private Object extractDispatcher(Object target) {
        return dispatcherExtractor.extractDispatcher(target);
    }
}
