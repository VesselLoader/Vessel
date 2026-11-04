package dev.vessel.bootstrap.wave8;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.VesselWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Wave 8 verifying Bukkit {@link ServicesManager},
 * {@link RegisteredServiceProvider}, {@link ServicePriority}, {@link Messenger},
 * {@link PluginMessageListener}, {@link World} (`v1.0.2`), {@link Chunk}, {@link Entity},
 * {@link EntityType}, {@link ChunkLoadEvent}, {@link ChunkUnloadEvent}, and
 * {@link EntitySpawnEvent}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveEightBukkitServicesMessengerChunkAndEntityTest
        implements PluginMessageListener {

    private int receivedPluginMessages;
    private String lastReceivedChannel;

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        receivedPluginMessages++;
        lastReceivedChannel = channel;
    }

    @Test
    void testBukkitServicesManagerPluginMessengerWorldChunksAndEntities() {
        ServicesManager servicesManager = Bukkit.getServicesManager();
        Messenger messenger = Bukkit.getMessenger();
        servicesManager.clear();
        messenger.clear();

        servicesManager.register(
                CharSequence.class,
                "LowEconomy",
                null,
                ServicePriority.Low);
        servicesManager.register(
                CharSequence.class,
                "VaultEconomy",
                null,
                ServicePriority.Highest);

        assertTrue(servicesManager.isProvidedFor(CharSequence.class));
        assertEquals("VaultEconomy", servicesManager.load(CharSequence.class));
        RegisteredServiceProvider<CharSequence> registration =
                servicesManager.getRegistration(CharSequence.class);
        assertNotNull(registration);
        assertEquals(ServicePriority.Highest, registration.getPriority());
        assertEquals("VaultEconomy", registration.getProvider());

        receivedPluginMessages = 0;
        lastReceivedChannel = null;
        messenger.registerOutgoingPluginChannel(null, "BungeeCord");
        messenger.registerIncomingPluginChannel(null, "BungeeCord", this);
        assertTrue(messenger.isOutgoingChannelRegistered("BungeeCord"));
        assertTrue(messenger.isIncomingChannelRegistered("BungeeCord"));

        World world = new VesselWorld("world");
        Location loc = new Location(world, 32.0D, 64.0D, -48.0D);
        Player player = new Player("Shiroz", loc);

        int dispatchedCount = messenger.dispatchIncomingMessage(
                "BungeeCord",
                player,
                "ConnectHub".getBytes(StandardCharsets.UTF_8));
        assertEquals(1, dispatchedCount);
        assertEquals(1, receivedPluginMessages);
        assertEquals("BungeeCord", lastReceivedChannel);

        Chunk chunk = world.getChunkAt(2, -3);
        assertNotNull(chunk);
        assertEquals(2, chunk.getX());
        assertEquals(-3, chunk.getZ());
        assertTrue(chunk.isLoaded());
        assertFalse(chunk.isForceLoaded());
        chunk.setForceLoaded(true);
        assertTrue(chunk.isForceLoaded());

        ChunkLoadEvent loadEvent = new ChunkLoadEvent(chunk, true);
        assertEquals(chunk, loadEvent.getChunk());
        assertEquals(world, loadEvent.getWorld());
        assertTrue(loadEvent.isNewChunk());

        ChunkUnloadEvent unloadEvent = new ChunkUnloadEvent(chunk);
        assertEquals(chunk, unloadEvent.getChunk());
        assertTrue(unloadEvent.isSaveChunk());
        unloadEvent.setSaveChunk(false);
        assertFalse(unloadEvent.isSaveChunk());

        Entity spawned = world.spawnEntity(loc, EntityType.IRON_GOLEM);
        assertNotNull(spawned);
        assertEquals(EntityType.IRON_GOLEM, spawned.getType());
        assertEquals(loc, spawned.getLocation());
        assertTrue(spawned.isValid());
        assertEquals(1, world.getEntities().size());

        EntitySpawnEvent spawnEvent = new EntitySpawnEvent(spawned);
        assertEquals(spawned, spawnEvent.getEntity());
        assertEquals(EntityType.IRON_GOLEM, spawnEvent.getEntityType());
        assertFalse(spawnEvent.isCancelled());
        spawnEvent.setCancelled(true);
        assertTrue(spawnEvent.isCancelled());

        servicesManager.clear();
        messenger.clear();
    }
}
