package dev.vessel.bootstrap.target;

import dev.vessel.command.TestEchoPluginCommand;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.VesselServer;
import org.bukkit.command.SimpleCommandMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test suite verifying dynamic console command route resolution without static command hardcoding.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public class VesselConsoleCommandRouteResolverTest {

    private VesselConsoleCommandRouteResolver resolver;
    private Server server;

    @BeforeEach
    void setUp() {
        resolver = new VesselConsoleCommandRouteResolver();
        server = new VesselServer("VESSEL-RouteTest", "1.21.4", "1.21.4");
        Bukkit.setServer(server);
    }

    @Test
    void testIdentifiesSovereignKernelCommandsAndClear() {
        assertTrue(resolver.shouldIntercept("cls"));
        assertTrue(resolver.shouldIntercept("clear"));
        assertTrue(resolver.shouldIntercept("vessel"));
        assertTrue(resolver.shouldIntercept("/vessel status"));
        assertTrue(resolver.shouldIntercept("geyser reload"));
        assertTrue(resolver.shouldIntercept("spark tps"));
        assertTrue(resolver.shouldIntercept("restart"));
    }

    @Test
    void testIdentifiesRegisteredBukkitCommandsDynamically() {
        SimpleCommandMap commandMap = server.getCommandMap();
        commandMap.register("test", new TestEchoPluginCommand("chunky"));

        assertTrue(resolver.shouldIntercept("chunky"));
        assertTrue(resolver.shouldIntercept("chunky help"));
        assertTrue(resolver.shouldIntercept("/chunky start"));
        assertTrue(resolver.shouldIntercept("ec subarg"));
    }

    @Test
    void testPassesThroughStandardVanillaCommands() {
        assertFalse(resolver.shouldIntercept("say Hello world"));
        assertFalse(resolver.shouldIntercept("stop"));
        assertFalse(resolver.shouldIntercept("op Steve"));
        assertFalse(resolver.shouldIntercept("deop Steve"));
        assertFalse(resolver.shouldIntercept("whitelist list"));
        assertFalse(resolver.shouldIntercept("gamemode creative"));
    }
}
