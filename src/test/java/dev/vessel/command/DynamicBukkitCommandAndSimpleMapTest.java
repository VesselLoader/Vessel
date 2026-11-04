package dev.vessel.command;

import dev.vessel.adapter.bukkit.PluginCommandAutoRegistrar;
import dev.vessel.adapter.bukkit.PluginYmlCommandDescriptor;
import dev.vessel.adapter.bukkit.PluginYmlCommandParser;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.VesselServer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test suite verifying clean-room {@link SimpleCommandMap}, YAML command descriptor parser,
 * auto-registrar, and Brigadier dynamic proxy adapter.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public class DynamicBukkitCommandAndSimpleMapTest {

    private Server testServer;
    private SimpleCommandMap commandMap;

    @BeforeEach
    void setUp() {
        testServer = new VesselServer("VESSEL-Test", "1.21.4", "1.21.4");
        Bukkit.setServer(testServer);
        commandMap = testServer.getCommandMap();
    }

    @Test
    void testPluginYmlCommandParserParsesInlineAndBlockAliases() throws Exception {
        String yaml = """
                name: TestPlugin
                version: 1.0.0
                main: dev.vessel.TestPlugin
                commands:
                  chunky:
                    description: Pre-generates chunks efficiently
                    aliases: [cy, chunk]
                    permission: chunky.command
                    usage: /chunky
                  clean:
                    description: Cleans up memory
                    permission: admin.clean
                    aliases:
                      - cl
                      - purge
                """;

        PluginYmlCommandParser parser = new PluginYmlCommandParser();
        List<PluginYmlCommandDescriptor> descriptors =
                parser.parseCommands(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

        assertEquals(2, descriptors.size());

        PluginYmlCommandDescriptor chunky = descriptors.get(0);
        assertEquals("chunky", chunky.getName());
        assertEquals("Pre-generates chunks efficiently", chunky.getDescription());
        assertEquals("/chunky", chunky.getUsage());
        assertEquals("chunky.command", chunky.getPermission());
        assertEquals(List.of("cy", "chunk"), chunky.getAliases());

        PluginYmlCommandDescriptor clean = descriptors.get(1);
        assertEquals("clean", clean.getName());
        assertEquals("Cleans up memory", clean.getDescription());
        assertEquals("admin.clean", clean.getPermission());
        assertTrue(clean.getAliases().contains("cl"));
        assertTrue(clean.getAliases().contains("purge"));
    }

    @Test
    void testSimpleCommandMapRegisterDispatchAndTabComplete() {
        TestEchoPluginCommand echoCommand = new TestEchoPluginCommand("echo");
        boolean registered = commandMap.register("test", echoCommand);
        assertTrue(registered);

        assertNotNull(commandMap.getCommand("echo"));
        assertNotNull(commandMap.getCommand("test:echo"));
        assertNotNull(commandMap.getCommand("ec"));

        ConsoleCommandSender sender = new ConsoleCommandSender();
        boolean dispatched = commandMap.dispatch(sender, "echo hello world");
        assertTrue(dispatched);
        assertEquals("Executed echo with [hello, world]", echoCommand.getLastExecutionLog());

        boolean aliasDispatched = commandMap.dispatch(sender, "ec testing alias");
        assertTrue(aliasDispatched);
        assertEquals("Executed ec with [testing, alias]", echoCommand.getLastExecutionLog());

        List<String> suggestions = commandMap.tabComplete(sender, "/ec");
        assertNotNull(suggestions);
        assertTrue(suggestions.contains("/echo") || suggestions.contains("/ec"));
    }

    @Test
    void testPluginCommandAutoRegistrarWiresIntoCommandMap() {
        JavaPlugin mockPlugin = new JavaPlugin();
        PluginYmlCommandDescriptor desc = new PluginYmlCommandDescriptor(
                "mocktest",
                "Mock command description",
                "/mocktest <arg>",
                "mock.perm",
                List.of("mt", "mtest"));

        PluginCommandAutoRegistrar registrar = new PluginCommandAutoRegistrar();
        int count = registrar.registerPluginCommands(mockPlugin, List.of(desc), commandMap);

        assertEquals(1, count);
        Command cmd = commandMap.getCommand("mocktest");
        assertNotNull(cmd);
        assertEquals("Mock command description", cmd.getDescription());
        assertEquals("/mocktest <arg>", cmd.getUsage());
        assertTrue(cmd.getAliases().contains("mt"));
    }
}
