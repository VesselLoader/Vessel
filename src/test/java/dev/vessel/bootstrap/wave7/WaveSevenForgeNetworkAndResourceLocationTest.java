package dev.vessel.bootstrap.wave7;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEventContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketTarget;
import net.minecraftforge.network.SimpleChannel;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Wave 7 verifying Minecraft {@link ResourceLocation} and Forge
 * {@link NetworkRegistry}, {@link SimpleChannel}, {@link PacketTarget},
 * {@link NetworkDirection}, and {@link NetworkEventContext}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveSevenForgeNetworkAndResourceLocationTest {

    private int workExecutionCounter;

    private static String supplyProtocolVersion() {
        return "1";
    }

    private static boolean acceptAnyVersion(String version) {
        return version != null;
    }

    public void recordEnqueuedWork() {
        workExecutionCounter++;
    }

    public void handleForgeStringMessage(String message, NetworkEventContext context) {
        context.enqueueWork(this::recordEnqueuedWork);
        context.setPacketHandled(true);
    }

    @Test
    void testResourceLocationAndForgeNetworkRegistrySimpleChannel() {
        ResourceLocation parsed = ResourceLocation.parse("vessel:quantum_core");
        assertEquals("vessel", parsed.getNamespace());
        assertEquals("quantum_core", parsed.getPath());
        assertEquals("vessel:quantum_core", parsed.toString());

        ResourceLocation defaultNs = ResourceLocation.withDefaultNamespace("diamond");
        assertEquals("minecraft", defaultNs.getNamespace());
        assertEquals("diamond", defaultNs.getPath());
        assertEquals("minecraft:diamond", defaultNs.toString());

        ResourceLocation fromParts =
                ResourceLocation.fromNamespaceAndPath("vessel", "quantum_core");
        assertEquals(parsed, fromParts);
        assertNotNull(ResourceLocation.tryParse("vessel:valid_key"));

        workExecutionCounter = 0;
        NetworkRegistry.clearChannels();
        SimpleChannel channel = NetworkRegistry.newSimpleChannel(
                "vessel:main",
                WaveSevenForgeNetworkAndResourceLocationTest::supplyProtocolVersion,
                WaveSevenForgeNetworkAndResourceLocationTest::acceptAnyVersion,
                WaveSevenForgeNetworkAndResourceLocationTest::acceptAnyVersion);

        assertNotNull(channel);
        assertSame(channel, NetworkRegistry.getChannel("vessel:main"));
        assertEquals("vessel:main", channel.getChannelName());
        assertEquals("1", channel.getProtocolVersion());

        channel.registerMessage(0, String.class, this::handleForgeStringMessage);

        channel.sendToServer("hello_forge");
        channel.send(PacketTarget.all(), "broadcast");
        assertEquals(2, channel.getSentMessages().size());

        UUID senderId = UUID.randomUUID();
        NetworkEventContext ctx =
                new NetworkEventContext(NetworkDirection.PLAY_TO_SERVER, senderId);
        assertEquals(NetworkDirection.PLAY_TO_SERVER, ctx.getDirection());
        assertEquals(senderId, ctx.getSenderUuid());

        boolean dispatched = channel.dispatch("hello_forge", ctx);
        assertTrue(dispatched);
        assertTrue(ctx.getPacketHandled());
        assertEquals(1, ctx.getEnqueuedWorkCount());
        assertEquals(1, workExecutionCounter);

        NetworkRegistry.clearChannels();
    }
}
