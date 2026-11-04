package dev.vessel.bootstrap.wave7;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourceType;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Wave 7 verifying Brigadier {@link CommandDispatcher},
 * {@link LiteralArgumentBuilder}, Fabric {@link CommandRegistrationCallback}, and
 * {@link ResourceManagerHelper} / {@link IdentifiableResourceReloadListener}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.2
 */
final class WaveSevenFabricResourceAndBrigadierCommandTest
        implements IdentifiableResourceReloadListener {

    private int reloadInvocationCount;

    @Override
    public String getFabricId() {
        return "vessel:wave7_server_data_reloader";
    }

    @Override
    public void onResourceReload() {
        reloadInvocationCount++;
    }

    public int executeVesselCommand(CommandContext<String> context) {
        return Command.SINGLE_SUCCESS;
    }

    public int executePingCommand(CommandContext<String> context) {
        return Command.SINGLE_SUCCESS;
    }

    @SuppressWarnings("unchecked")
    public void registerCommandsOnCallback(
            Object dispatcherObj,
            Object registryAccess,
            boolean dedicatedServer) {
        if (dispatcherObj instanceof CommandDispatcher<?> rawDispatcher && dedicatedServer) {
            CommandDispatcher<String> typedDispatcher = (CommandDispatcher<String>) rawDispatcher;
            typedDispatcher.register(
                    LiteralArgumentBuilder.<String>literal("ping")
                            .executes(this::executePingCommand));
        }
    }

    @Test
    void testFabricCommandRegistrationResourceManagerHelperAndBrigadierDispatcher() throws Exception {
        CommandDispatcher<String> dispatcher = new CommandDispatcher<>();
        dispatcher.register(
                LiteralArgumentBuilder.<String>literal("vessel")
                        .executes(this::executeVesselCommand));
        assertEquals(1, dispatcher.execute("vessel", "ServerConsole"));

        CommandRegistrationCallback.EVENT.clearListeners();
        CommandRegistrationCallback.EVENT.register(this::registerCommandsOnCallback);
        CommandRegistrationCallback.EVENT.invoker().register(dispatcher, "RegistryAccess", true);
        assertEquals(1, dispatcher.execute("ping", "ServerConsole"));
        CommandRegistrationCallback.EVENT.clearListeners();

        reloadInvocationCount = 0;
        ResourceManagerHelper serverDataHelper =
                ResourceManagerHelper.get(ResourceType.SERVER_DATA);
        serverDataHelper.clearListeners();
        assertEquals(ResourceType.SERVER_DATA, serverDataHelper.getResourceType());

        serverDataHelper.registerReloadListener(this);
        assertTrue(serverDataHelper.isRegistered("vessel:wave7_server_data_reloader"));
        assertEquals(1, serverDataHelper.triggerReloadAll());
        assertEquals(1, reloadInvocationCount);
        serverDataHelper.clearListeners();
    }
}
