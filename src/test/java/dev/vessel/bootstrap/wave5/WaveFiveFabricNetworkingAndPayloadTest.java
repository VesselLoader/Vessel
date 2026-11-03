package dev.vessel.bootstrap.wave5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.CustomPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayPayloadContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Wave 5 verifying Fabric Networking API
 * ({@link PayloadTypeRegistry}, {@link ServerPlayNetworking}, {@link PlayPayloadContext},
 * and {@link CustomPayload}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveFiveFabricNetworkingAndPayloadTest {

    public void handleIncomingPingPayload(
            SampleFabricCustomPayloadFixture payload,
            PlayPayloadContext context) {
        context.sendResponse(new SampleFabricCustomPayloadFixture("pong:" + payload.message()));
    }

    @Test
    void testFabricPayloadTypeRegistryAndServerPlayNetworking() {
        PayloadTypeRegistry.playC2S().clear();
        PayloadTypeRegistry.playS2C().clear();
        ServerPlayNetworking.clearReceivers();

        PayloadTypeRegistry.playC2S().register(
                SampleFabricCustomPayloadFixture.ID,
                SampleFabricCustomPayloadFixture.class);
        PayloadTypeRegistry.playS2C().register(
                SampleFabricCustomPayloadFixture.ID,
                SampleFabricCustomPayloadFixture.class);

        assertTrue(PayloadTypeRegistry.playC2S().isRegistered(SampleFabricCustomPayloadFixture.ID));
        assertTrue(PayloadTypeRegistry.playS2C().isRegistered("vessel:wave5_ping"));

        boolean registered = ServerPlayNetworking.registerGlobalReceiver(
                SampleFabricCustomPayloadFixture.ID,
                this::handleIncomingPingPayload);
        assertTrue(registered);

        UUID playerUuid = UUID.randomUUID();
        PlayPayloadContext context = new PlayPayloadContext(playerUuid, "Shiroz");
        assertEquals(playerUuid, context.playerUuid());
        assertEquals("Shiroz", context.playerName());

        SampleFabricCustomPayloadFixture request = new SampleFabricCustomPayloadFixture("hello");
        assertEquals("hello", new String(request.encodePayloadBytes(), StandardCharsets.UTF_8));

        boolean dispatched = ServerPlayNetworking.dispatchIncomingPayload(request, context);
        assertTrue(dispatched);
        assertTrue(ServerPlayNetworking.canSend(playerUuid, SampleFabricCustomPayloadFixture.ID));

        List<CustomPayload> responses = context.getSentResponses();
        assertEquals(1, responses.size());
        SampleFabricCustomPayloadFixture reply =
                (SampleFabricCustomPayloadFixture) responses.get(0);
        assertEquals("pong:hello", reply.message());
    }
}
