package dev.vessel.bootstrap.wave11;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.vessel.api.proxy.VesselProxySessionSnapshot;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

/**
 * Integration test verifying Velocity and BungeeCord proxy surfaces and VUSA snapshot interop.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveElevenVelocityAndBungeeProxyTest {

    @Test
    void testVelocityProxySurfaceAndEvents() {
        Plugin pluginAnn = SampleVelocityPluginFixture.class.getAnnotation(Plugin.class);
        assertNotNull(pluginAnn);
        assertEquals("test-velocity", pluginAnn.id());
        assertEquals("Test Velocity Plugin", pluginAnn.name());

        UUID playerUuid = UUID.randomUUID();
        SampleVelocityPlayerDouble player = new SampleVelocityPlayerDouble(playerUuid, "Shiroz");
        assertEquals("Shiroz", player.getUsername());
        assertEquals(playerUuid, player.getUniqueId());
        assertTrue(player.isActive());

        PostLoginEvent postLogin = new PostLoginEvent(player);
        assertEquals(player, postLogin.getPlayer());

        ProxyInitializeEvent initEvent = new ProxyInitializeEvent();
        assertNotNull(initEvent);

        VesselProxySessionSnapshot snapshot = new VesselProxySessionSnapshot(
                player.getUniqueId(),
                player.getUsername(),
                "VELOCITY",
                "hub-1",
                20L);
        assertEquals("Shiroz", snapshot.username());
        assertTrue(snapshot.isConnectedTo("hub-1"));
    }

    @Test
    void testBungeeCordProxySurfaceAndEvents() {
        UUID bId = UUID.randomUUID();
        SampleBungeePlayerDouble bPlayer = new SampleBungeePlayerDouble(bId, "Shiroz");
        assertEquals("Shiroz", bPlayer.getName());
        assertEquals(bId, bPlayer.getUniqueId());
        assertTrue(bPlayer.isConnected());

        net.md_5.bungee.api.ProxyServer proxy = net.md_5.bungee.api.ProxyServer.getInstance();
        proxy.registerPlayer(bPlayer);
        assertEquals(bPlayer, proxy.getPlayer(bId));
        assertEquals(bPlayer, proxy.getPlayer("Shiroz"));

        net.md_5.bungee.api.event.PostLoginEvent bPostLogin = new net.md_5.bungee.api.event.PostLoginEvent(bPlayer);
        assertEquals(bPlayer, bPostLogin.getPlayer());

        proxy.unregisterPlayer(bId);
    }
}
